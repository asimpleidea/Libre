// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database.
const account = require("./mad24-admin.json");
const admin = require('firebase-admin');
const mkdirp = require('mkdirp-promise');
const gcs = require('@google-cloud/storage')(({keyFilename: './mad24-admin.json'}));
admin.initializeApp({
    credential: admin.credential.cert(account),
    databaseURL: "https://mad24-ac626.firebaseio.com"
  });
const spawn = require('child-process-promise').spawn;
const path = require('path');
const os = require('os');
const fs = require('fs');

exports.debugCreateChat = functions.https.onRequest((req, res) => 
{
    const first = "first_user",
          second = "second_user",
          chat_id = "book_id:" + first + "&" + second;

    const partecipants = 
    {
        first:
        {
            is_typing: false,
            last_here: "2018-05-18T14:42:30Z"
        },
        second:
        {
            is_typing: false,
            last_here: "0"
        }
    };

    return Promise.all([    admin.firestore().doc("chat_messages/" + chat_id + "/partecipants/" + first).set(partecipants.first),
                            admin.firestore().doc("chat_messages/" + chat_id + "/partecipants/" + second).set(partecipants.second)  ])
                .then(results =>
                {
                    return res.sendStatus(200);
                });           

});

exports.debugCreateBorrowing = functions.https.onRequest((req, res) => 
{
    const book_id = "book_id",
            borrowing_id = "borrowing_id";

    return admin.database().ref("borrowings/" + book_id + "/" + borrowing_id).set({book_id: book_id, 
                                                                                    from : "owner_id", 
                                                                                    to: "borrower_id",
                                                                                    started_at: (new Date).getTime(),
                                                                                    returned_at: "0" }).then(result =>
                                                                                        {
                                                                                            return res.sendStatus(200);
                                                                                        });
});

exports.debugChangeBorrowingID = functions.https.onRequest((req, res) => 
{
    const book_id = "-LCAV7NE3xYParkQc9Lt",
            i = ('i' in req.query ? req.query.i : "id1/id2"); 

    return admin.database().ref("books/" + book_id).update({borrowing_id: i}).then(result => 
        {
            return res.sendStatus(200);
        });
});

exports.debugUpdateStatus = functions.https.onRequest((req, res) => 
{
    const chat_id = "book_id:first_user&second_user",
            date = new Date(),
            me = "first_user",
            them = "second_user";
    
    return admin.firestore().doc("chat_messages/" + chat_id + "/partecipants/" + me).update({last_here: (date.toISOString().split('.')[0]+'Z')})
    .then(s => 
        {
            return res.sendStatus(200);
        });
});

exports.createChat = functions.firestore.document('/chat_messages/{chatId}/partecipants/{userId}').onCreate((snap, context) => 
{
    //  Original value
    const   chatId = context.params.chatId,
            user = context.params.userId,
            book_id = chatId.split(':')[0],
            users = chatId.split(':')[1].split('&');

    if(users.length !== 2) return console.log("error: id is not correct");

    const otherUser = user === users[0] ? users[1] : users[0];

    const chat_promise = admin.firestore().doc("chats/" + user + "/conversations/" + chatId).set({chat_id : chatId, partner_id: otherUser, book_id : book_id}),
            book_chat = admin.firestore().doc("book_chats/" + book_id + "/conversations/" + chatId).set({id: chatId});

    return Promise.all([ chat_promise, book_chat ]);
        });

exports.updateAvailability = functions.database.ref('/books/{book_id}/borrowing_id').onWrite((change, context) => 
{
    //  Deleted?
    if (!change.after.exists()) return null;

    //  Start
    let available = true,
        borrowing_id = ""
        returned_at = 0;
    const original = change.after.val(),
        book_id = context.params.book_id;
    

    //  If now has an ID then it is not available
    if(original.length > 0) 
    {
        available = false;
        borrowing_id = original.split('/')[1];
    }
    else
    {
        //  The value before
        const before = change.before.val();
        if(before.length > 0)
        {
            borrowing_id = before.split('/')[1];
            returned_at = (new Date()).getTime();
        }
    }
    
    //  The promises
    const promises = [];

    //  Get the user
    const update_available = admin.database().ref("books/" + context.params.book_id).once("value").then(result => 
    {
        //  Get the user
        const user = result.val().user_id;

        //  Update availability
        return admin.database().ref("users/" + user + "/books/" + book_id).set(available);
    });
    promises.push(update_available);

    //  update borrowed books
    if(borrowing_id.length > 0)
    {
        const update_borrowed =  admin.database().ref("borrowings/" + book_id + "/" + borrowing_id).once("value").then(result =>
        {
            const data = result.val(),
                    update = {};
                    update[book_id] = !available ? borrowing_id : null;                
    
            return admin.database().ref("users/" + data.to + "/borrowed_books").update(update);
        });
        promises.push(update_borrowed);

        //  Borrowing ended?
        if(returned_at > 0)
        {
            const update_returned_at = admin.database().ref("borrowings/" + book_id + "/" + borrowing_id).update({returned_at : returned_at});
            promises.push(update_returned_at);

            const update_borrowed =  admin.database().ref("borrowings/" + book_id + "/" + borrowing_id).once("value").then(result =>
            {
                const data = result.val(),
                        update = {};
                        update[book_id] = !available ? borrowing_id : null;

                return admin.database().ref("users/" + data.to + "/books_to_rate/"+ book_id).set(borrowing_id);
            });
        }
    }

    return Promise.all(promises);
});

//  Update rating inserted by the owner
exports.addRatingByOwner = functions.database.ref('/borrowings/{book_id}/{borrowing_id}/owner_rating').onCreate((snap, context) => 
{
    //  Get the data
    const original = snap.val(),
    
            //  Get the stars
            stars = original.stars,
    
            //  What book are we talking about?
            book_id = context.params.book_id,

            //  What is the borrowing id?
            borrowing_id = context.params.borrowing_id;

    //  Get the user we want to rate.
    return admin.database().ref("/borrowings/" + book_id + "/" + borrowing_id).once("value").then(result =>
    {
        if(result === null) return console.log("the value was null!");

        //  Get the data
        const user = result.val().to;

        const owner = result.val().from;

        if(original.comment)
            comment = original.comment;
        else
            comment = "";

        updateComments(user, borrowing_id, owner, stars, comment);


        return updateRating(user, stars);
    });
});

//  Update rating inserted by the borrower
exports.addRatingByBorrower = functions.database.ref('/borrowings/{book_id}/{borrowing_id}/borrower_rating').onCreate((snap, context) => 
{
    //  Get the data
    const original = snap.val(),

    //  The book
    book_id = context.params.book_id,

    //  The borrowing id
    borrowing_id = context.params.borrowing_id,

    //  How many stars
    stars = original.stars;

    //  Get the owner that I am rating
    return admin.database().ref("/borrowings/" + book_id + "/" + borrowing_id).once("value").then(result =>
    {
        if(result === null) return console.log("the value was null!");

        const owner = result.val().from;

        const borrower = result.val().to;

        if(original.comment)
            comment = original.comment;
        else
            comment = "";
        updateComments(owner, borrowing_id, borrower, stars, comment);

        removeBookToRate(borrower, book_id);

        return updateRating(owner, stars);
    })
});

function updateRating(userToUpdate, stars)
{
    //  Do it in a transaction
    return admin.database().ref("users/" + userToUpdate).transaction(u =>
    {
        //  Sometimes it just is null, so I have to do this!
        if(u === null)
        {
            return {transaction: null};
        }

        //  Update the stars (with reset in case they had no rating)
        if(!("rating" in u)) u.rating = 0;
        u.rating += parseInt(stars);

        //  Update how many rated them
        if(!("raters" in u)) u.raters = 0;
        u.raters = parseInt(u.raters) +1;

        return u;
    });
}

function updateComments(userToUpdate, borrowingId, commentWriter, stars, comment)
{
    //  Do it in a transaction
    return admin.database().ref("comments/" + userToUpdate + "/" + borrowingId).set({"user": commentWriter,
                                                                                "stars": stars,
                                                                                "comment": comment
                                                                                });
}

function removeBookToRate(borrowerId, bookId)
{
    //  Do it in a transaction
    return admin.database().ref("users/" + borrowerId + "/books_to_rate/" + bookId).remove();
}

exports.replicateStatus = functions.firestore.document('/chat_messages/{chatId}/partecipants/{userId}').onUpdate((change, context) => 
{
    // Get an object representing the document
    const newValue = change.after.data();

    // ...or the previous value before this update
    const previousValue = change.before.data();

    // if not changed don't do anything
    if(previousValue.last_here === newValue.last_here) return;

    const user_id = context.params.userId,
            chat_id = context.params.chatId
            users = chat_id.split(':')[1].split('&'),
            otherUser = user_id === users[0] ? users[1] : users[0],
            my_last_here = newValue.last_here;

    return admin.firestore().doc("chats/" + user_id + "/conversations/" + chat_id).update({my_last_here : my_last_here});
});

exports.debugPostMessage = functions.https.onRequest((req, res) => 
{
    const chat_id = "-LCiMkny1kg--dDZiN4e:6GlvXy7stFPDYzheZ7It5Nob0tf1&qc8wMfGLKfPgwqcF7ls0wQXjYkv2";
    var date = new Date();
    
    const message = 
    {
        by: (('by' in req.query) ? "6GlvXy7stFPDYzheZ7It5Nob0tf1" : "qc8wMfGLKfPgwqcF7ls0wQXjYkv2"),
        content: (!('content' in req.query) ? "new content" : req.query.content),
        sent: (date.toISOString().split('.')[0]+'Z'),
        received: '0'
    };
            
    return admin.firestore().collection("chat_messages/" + chat_id + "/messages").doc().set(message)
    .then(s => 
    {
        return res.sendStatus(200);
    });
});

exports.debugPostTestMessage = functions.https.onRequest((req, res) => 
{
    const chat_id = "book_id:first_user&second_user";
    var date = new Date();
    
    const message = 
    {
        by: (!('by' in req.query) ? "first_user" : "second_user"),
        content: (!('content' in req.query) ? "new content" : req.query.content),
        sent: (date.toISOString().split('.')[0]+'Z'),
        received: '0'
    };
            
    return admin.firestore().collection("chat_messages/" + chat_id + "/messages").doc().set(message)
    .then(s => 
    {
        return res.sendStatus(200);
    });
});

exports.updateChat = functions.firestore.document('/chat_messages/{chatId}/messages/{messageId}').onCreate((snap, context) => 
{
    const chat_id = context.params.chatId,
            message_id = context.params.messageId,
            message = snap.data()
            users = chat_id.split(':')[1].split('&');

    //  Build the object
    const data = 
    {
        last_message_id: message_id,
        last_message_time: message.sent,
        last_message_by: message.by,
        preview: message.content
    };

    //  Create them
    return Promise.all([    admin.firestore().doc("chats/" + users[0] + "/conversations/" + chat_id).get(),
                            admin.firestore().doc("chats/" + users[0] + "/conversations/" + chat_id).update(data),
                            admin.firestore().doc("chats/" + users[1] + "/conversations/" + chat_id).update(data)] )
            .then(results =>
                {
                    const r = results[0],
                        newDay = message.sent.split('T')[0];
                        let needFirst = false;

                    if (r.exists && 'last_message_time' in r.data())
                    {
                        if(r.data().last_message_time.split('T')[0] !== newDay) needFirst = true;
                    }
                    else 
                    {
                        console.log("no messages there");
                        needFirst = true;
                    }

                    return needFirst;
                })
                .then( first => 
                    {
                    
                        if(!first) return;

                        return admin.firestore().doc("/chat_messages/" + chat_id + "/messages/" + message_id).update({firstOfTheDay : true});
                    
                    });
});

exports.sendNotification = functions.firestore.document('/chat_messages/{chatId}/messages/{messageId}').onCreate((snap, context) => 
{
    const chat_id = context.params.chatId,
            message_id = context.params.messageId,
            message = snap.data(),
            users = chat_id.split(':')[1].split('&'),
            book_id = chat_id.split(':')[0],
            sender = message.by,
            receiver = sender === users[0] ? users[1] : users[0];

    return Promise.all([    admin.database().ref("users/" + receiver).once("value"),
                            admin.database().ref("users/" + sender).once("value") ])
            .then(results =>
            {
                if(results[0] === null || results[1] === null) return false;

                const   r = results[0].val(),
                        s = results[1].val();
                
                if(!("device_token" in r)) return console.log("user has not device token");

                //  Already in this chat?
                if("status" in r)
                {
                    if(r.status === "online" && r.in_chat === chat_id) return console.log("user is already in this chat");
                }

                //  Image type
                const picType = ( function()
                {
                    if('thumbnail_exists' in s) return "thumb";
                    if('has_image' in s) return "original";
                    return "uknown";
                }());

                //  Get token
                const token = r.device_token;

                //  Build the payload
                const payload = 
                {
                    notification: 
                    {
                        title: s.name,
                        body: message.content,
                    },
                    data:
                    {
                        partner_name: s.name,
                        chat_id : chat_id,
                        last_message_id: message_id,
                        last_message_time: message.sent,
                        partner_id : sender,
                        partner_image : picType,
                        book_id : book_id
                    }
                };

                //  Ok, send the notification
                return admin.messaging().sendToDevice(token, payload);
            });
});

//  Generate avatar thumb
exports.generateThumbnail = functions.storage.bucket().object().onFinalize(object =>
{
  // File and directory paths
  const filePath = object.name,

        //  The MIME type
        contentType = object.contentType,

        //  File directory
        fileDir = path.dirname(filePath),

        //  The file name
        fileName = path.basename(filePath),

        //  The thumb path
        thumbFilePath = path.normalize(path.join(fileDir, "thumb_" + fileName)),

        //  Temp local file
        tempLocalFile = path.join(os.tmpdir(), filePath),

        //  Temp directory
        tempLocalDir = path.dirname(tempLocalFile),

        //  Temp thumb
        tempLocalThumbFile = path.join(os.tmpdir(), thumbFilePath);

        //  Is this an image?
        if (!contentType.startsWith('image/')) return null;

        //  Already a thumbnail?
        if (fileName.startsWith("thumb_")) return null;

        // Cloud Storage files.
        const bucket = gcs.bucket(object.bucket),
                file = bucket.file(filePath),
                thumbFile = bucket.file(thumbFilePath),
                metadata = { contentType: contentType };

        // Create the temp directory where the storage file will be downloaded.
        return mkdirp(tempLocalDir).then(() => 
        {
            // Download file from bucket.
            return file.download({destination: tempLocalFile});
        })
        .then(() => 
        {
            //console.log('The file has been downloaded to', tempLocalFile);
            
            // Generate a thumbnail using ImageMagick.
            return spawn('convert', [tempLocalFile, '-thumbnail', `150x150>`, tempLocalThumbFile], {capture: ['stdout', 'stderr']});
        })
        .then(() => 
        {
            //console.log('Thumbnail created at', tempLocalThumbFile);
    
            // Uploading the Thumbnail.
            return bucket.upload(tempLocalThumbFile, {destination: thumbFilePath, metadata: metadata});
        })
        .then(() => 
        {
            //console.log('Thumbnail uploaded to Storage at', thumbFilePath);

            // Once the image has been uploaded delete the local files to free up disk space.
            fs.unlinkSync(tempLocalFile);
            fs.unlinkSync(tempLocalThumbFile);

            // Get the Signed URLs for the thumbnail and original image.
            /*const config = {
            action: 'read',
            expires: '03-01-2500',
            };
            return Promise.all([
                thumbFile.getSignedUrl(config),
                file.getSignedUrl(config),
                ]);*/
            return Promise.resolve(thumbFile.exists());
        })
        .then(result =>
        {
            if(!result) return false;
            
            //  Get the id
            const id = fileName.split(".")[0];

            //  Set the thumb
            if(fileDir === "profile_pictures")  return admin.database().ref("users/" + id).update({has_image: true, thumbnail_exists : true});

            return admin.database().ref("books/" + id).update({has_image: true, thumbnail_exists : true});
        })
        /*.then((results) => 
        {
            console.log('Got Signed URLs.');
            const thumbResult = results[0];
            const originalResult = results[1];
            const thumbFileUrl = thumbResult[0];
            const fileUrl = originalResult[0];

            // Add the URLs to the Database
            //return admin.database().ref('images').push({path: fileUrl, thumbnail: thumbFileUrl});
            return console.log("thumbUrl: " + thumbFileUrl + ", fileUrl: " + fileUrl);
        })*/;
});

//  Update rating inserted by the borrower
exports.removeBook = functions.database.ref('/booksTest/{book_id}').onDelete((snap, context) => {

    //  Get book to remove
    const original = snap.val(),

    //  get book id
    book_id = context.params.book_id,

    //  get owner id
    owner = original.user_id,

    //  Set the location books promise
    location_book = admin.database().ref("/locationBooksTest/" + book_id).remove(),
    
    //  Set the user books promise
    owner_book = admin.database().ref("/users/" + owner + "/books/" + book_id).remove(),
    
    //  The storage bucket 
    bucket = gcs.bucket("mad24-ac626.appspot.com"),

    //  Set the images references
    //  Note: not all of our books have a has_image field. So I can't check that, unfortunately
    //  Note: this will throw an error if the image does not exist. And who cares? Just don't do anything then.
    image_delete = bucket.file("bookCovers/" + book_id + ".jpg").delete(),
    thumb_delete = bucket.file("bookCovers/thumb_" + book_id + ".jpg").delete(),

    //  Get all the chats about this book
    //  NOTE: Unfortunately, firebase (and firestore) has no concept of soft delete (or logical delete).
    //  So, I have to delete what's inside an object before actually deleting that object. 
    //  Feels bad, man.
    chat_book = admin.firestore().collection("book_chats/" + book_id + "/conversations/").get().then(results => 
    {
        //  Chats messages promises
        const chats_messages = [],

        //  Chats promises
        chats = [],

        //  Books promise
        book_collection = [],

        doc_ids = [];

        results.forEach( doc => 
        {
            let users = doc.id.split(":")[1].split("&");

            //  Get the ids
            doc_ids.push(doc.id);

            //  Delete what's inside the book collection
            book_collection.push(admin.firestore().doc("book_chats/" + book_id + "/conversations/" + doc.id).delete());

            //  Delete the partecipants object
            chats_messages.push(admin.firestore().doc("chat_messages/" + doc.id + "/partecipants/" + users[0]).delete());
            chats_messages.push(admin.firestore().doc("chat_messages/" + doc.id + "/partecipants/" + users[1]).delete());

            //  Delete the conversations
            chats.push(admin.firestore().doc("chats/" + users[0] + "/conversations/" + doc.id).delete());
            chats.push(admin.firestore().doc("chats/" + users[1] + "/conversations/" + doc.id).delete());
        });

        return Promise.all([chats_messages, chats, book_collection, doc_ids]);    
    }).then(results => 
    {
        //  Get the ids from the previous promise
        const ids = results[3],

        //  The queries
        queries = [];

        if(!Array.isArray(ids) || (Array.isArray(ids) && ids.length < 1)) 
        {
            console.log("error while trying to query messages to delete");
            return null;
        }
        
        ids.forEach(id => 
        {
            queries.push(admin.firestore().collection("chat_messages/" + id + "/messages").get());
        });

        return Promise.all(queries);
    }).then(results => 
    {
        if(results === null) return console.log("exiting removeBook because results is null");

        //  The messages promises
        const messages = [];

        results.forEach(r => 
        {
            r.forEach(d => 
            {
                messages.push(d.ref.delete());
            });            
        });

        return Promise.all(messages);
    });

    //  Ok now do the actual stuff, all asynchronously
    return Promise.all([    location_book, 
                            owner_book,
                            image_delete,
                            thumb_delete,
                            chat_book   ]);

    //remove book from books array
    /*admin.database().ref("/users/"+ owner +"/books/"+ book_id).remove();

    //remove book cover --> non funziona per ora
    gcs.bucket("gs://mad24-ac626.appspot.com").file("/bookCovers/"+ book_id +".jpg").delete();
    gcs.bucket("gs://mad24-ac626.appspot.com").file("/bookCovers/thumb_"+ book_id +".jpg").delete();*/

    /*//remove book from books_to_rate array --> non funziona per ora
    //.ref("/users/").orderByChild("books_to_rate").equalTo(book_id)
    admin.database().once('value').then(function(snapshot) {
        snapshot.forEach(function(childSnapshot) {
            //get user
            user = childSnapshot.val();

            user.books_to_rate.forEach(function(book) {
                b = book.val();

                if(b == book_id)
                    book.ref.remove();

                return;
            });
        });

        return;
    });*/
});
