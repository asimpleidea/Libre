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

//  On partecipants created
/*exports.generateChat = functions.firestore.document("chat_messages/{$chatId}/partecipants/{$userId}").onCreate((snapshot, context) => 
{
    //  Original Value
    const original = snapshot.data(),
            chatId = context.params.chatId,
            user = context.params.userId,
            users = chatId.split('&');

    return console.log("original", original);
    if(users.length != 2) return console.log("error: id is not correct");

    const otherUser = user === users[0] ? user[1] : user[0];

    const data = {};
    data[otherUser] =   {
                            chat_id : chatId
                        };

    return admin.firestore().document("chats/" + user).update(data);
});*/

exports.createChat = functions.firestore.document('/chat_messages/{chatId}/partecipants/{userId}').onCreate((snap, context) => 
{
    //  Original value
    const original = snap.data();
            chatId = context.params.chatId,
            user = context.params.userId,
            users = chatId.split('&');

    if(users.length !== 2) return console.log("error: id is not correct");

    const otherUser = user === users[0] ? user[1] : user[0];

    return admin.firestore().doc("chats/" + user + "/conversations/" + otherUser).set({chat_id : chatId});
});

//  On New message
exports.updateChat = functions.database.ref('/chat_messages/{chatId}/messages/{messageId}').onCreate((snapshot, context) => 
{
    //  Original Value
    const original = snapshot.val(),
            chatId = context.params.chatId,
            messageId = context.params.messageId;


    return admin.database().ref("chat_messages/" + chatId + "/partecipants").once("value").then(snapshot =>
    {
        //  Get the users
        const users = Object.keys(snapshot.val());

        //  Wrong key number? this is actually impossible but who knows
        if(users.length < 2 || users.length > 2) return false;

        //  Generate the object
        const last_message = {
                                by : original.by,
                                preview : original.content,
                                id :  messageId,
                                time : original.sent 
                            };

        //  Replicate
        return Promise.all([    admin.database().ref("chats/" + users[0] + "/" + users[1] + "/last_message").update(last_message),
                                admin.database().ref("chats/" + users[1] + "/" + users[0] + "/last_message").update(last_message) ]);
    });
});

//  Send notifications
exports.sendNotification = functions.database.ref("/chat_messages/{chatId}/messages/{messageId}").onCreate((snapshot, context) =>
{
    const original = snapshot.val(),
            src = original.by,

    //  Get the data of the src
    srcDataPromise = admin.database().ref("users/" + src).once("value"),
    
    //  Get the user to be notified
    partecipantsPromise = admin.database().ref("/chat_messages/" + context.params.chatId + "/partecipants").once("value");

    //  Do something with those promises
    return Promise.all([srcDataPromise, partecipantsPromise]).then(results => 
    {
        const srcData = results[0].val(),
                partecipants = results[1].val(),
                users = Object.keys(partecipants),
                dest = users[0] === src ? users[1] : users[0];

        console.log("srcData: ", srcData);

        return admin.database().ref("users/" + dest).once("value")
        .then((_d) =>
        {
            //  Has the device token?
            if(!_d.hasChild("device_token")) return true;
            if(_d.hasChild("status"))
            {
                //  Don't send the notification if the user is in this chat
                if(_d.child("status") === "online" && _d.child("in_chat") === context.params.chatId) return true;
            }



            //  Image
            const picType = ( function()
            {
                if('thumbnail_exists' in srcData) return "thumb";
                if('has_image' in srcData) return "original";
                return "uknown";
            }());

            //  Get data
            const token = _d.child("device_token").val(),

            //  Build the payload
            payload = 
            {
                notification: 
                {
                    title: srcData.name,
                    body: original.content,
                },
                data:
                {
                    partner_name: srcData.name,
                    chat_id : context.params.chatId,
                    last_message_id: context.params.messageId,
                    last_message_time: original.sent,
                    partner_id : src,
                    partner_image : picType
                }
            };

            console.log("payload", payload);

            //  Ok, send the notification
            return admin.messaging().sendToDevice(token, payload);

        }); 
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
