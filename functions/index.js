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
          chat_id = first + "&" + second;

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

exports.createChat = functions.firestore.document('/chat_messages/{chatId}/partecipants/{userId}').onCreate((snap, context) => 
{
    //  Original value
    const   chatId = context.params.chatId,
            user = context.params.userId,
            users = chatId.split('&');

    if(users.length !== 2) return console.log("error: id is not correct");

    const otherUser = user === users[0] ? users[1] : users[0];

    return admin.firestore().doc("chats/" + user + "/conversations/" + otherUser).set({chat_id : chatId, partner_id: otherUser});
});

exports.debugPostMessage = functions.https.onRequest((req, res) => 
{
    const chat_id = "6GlvXy7stFPDYzheZ7It5Nob0tf1&P1f7ozEcOQNgHvuMXL4tSDxapgF3";
    var date = new Date();
    
    const message = 
    {
        by: (('by' in req.query) ? "6GlvXy7stFPDYzheZ7It5Nob0tf1" : "P1f7ozEcOQNgHvuMXL4tSDxapgF3"),
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
    const chat_id = "first_user&second_user";
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
            users = chat_id.split('&');

    //  Build the object
    const data = 
    {
        last_message_id: message_id,
        last_message_time: message.sent,
        last_message_by: message.by,
        preview: message.content
    };

    //  Create them
    return Promise.all([    admin.firestore().doc("chats/" + users[0] + "/conversations/" + users[1]).update(data),
                            admin.firestore().doc("chats/" + users[1] + "/conversations/" + users[0]).update(data)] );
});

exports.sendNotification = functions.firestore.document('/chat_messages/{chatId}/messages/{messageId}').onCreate((snap, context) => 
{
    const chat_id = context.params.chatId,
            message_id = context.params.messageId,
            message = snap.data(),
            users = chat_id.split('&'),
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
                        partner_image : picType
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
