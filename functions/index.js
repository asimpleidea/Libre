// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database.
const admin = require('firebase-admin');
admin.initializeApp();

//  On partecipants created
exports.createChat = functions.database.ref('/chat_messages/{chatId}/partecipants').onCreate((snapshot, context) => 
{
    //  Original Value
    const original = snapshot.val(),
            chatId = context.params.chatId,
            users = Object.keys(original);

    //  Wrong key number? this is actually impossible but who knows
    if(users.length < 2 || users.length > 2) return false;

    //  Replicate for the first user
    const first = {};
    first[users[1]] = {chat : chatId};
    
    //  Replicate the second user    
    const second = {};
    second[users[0]] = {chat: chatId};
    
    return Promise.all([ admin.database().ref("chats/" + users[0] + "/").update(first), admin.database().ref("chats/" + users[1] + "/").update(second)]);
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

        console.log("srcData: " + srcData);
        console.log("partecipants: " + partecipants);

        //  Get the device token of the dest
        return admin.database().ref("users/" + dest).once("value").then(_d =>
        {
            //  Has the device token?
            if(!_d.hasChild("device_token")) return console.log("no token found");

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
                    sender_name: srcData.name,
                    chat_id : context.params.chatId,
                    last_message_id: context.params.messageId,
                    last_message_time: original.sent,
                }
          };

            //  Ok, send the notification
            return admin.messaging().sendToDevice(token, payload);
        });
    });
});