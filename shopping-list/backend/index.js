const express = require('express');
const cors = require('cors'); // access our server from different domains
const bodyParser = require('body-parser'); // mainly used for post form
const mongoose = require('mongoose');
const ip = require('ip');
const crdt = require('react-crdt');
const e = require('express');
const path = require('path');
require('dotenv/config');

var clients = []; // list of clients
var tempState = {}; // list of CRDT objects
var clientResponseDict = {}; // list of client responses
var pollingQueue = {}; // list of client queues

const app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: false}));

app.set('trust proxy', true);


const corsOptions = {
    origin: "*",
    credentials:true,
    optionSuccessStatus:200
}

app.use(cors(corsOptions));

app.get('/', (req, res) => {
    console.log("GET REQUEST");

    var remIP =  req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    console.log("Client IP: " + remIP);

    if (clients.indexOf(remIP) == -1) {
        clients.push(remIP);
        console.log("Added client: " + remIP);
    } 
    
    else {
        console.log("Client already exists");
        console.log("Clients:" , clients);
    }
    
    res.sendFile(path.join(__dirname , '../frontend/public/index.html'));
})

//app.use(express.static(path.join(__dirname , '../frontend/public')));

app.post('/api', function(req, res) {
    console.log("API POST REQUEST");

    const clientIP = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    console.log("Client IP: " + clientIP);

    sendToAllClientsExceptSender(clientIP, req.body);

    res.statusCode = 200;
    res.end();
})

app.get('/api/lp', function(req, res) {
    console.log("API LONG POLLING REQUEST");
    const clientIP = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    console.log("Client IP: " + clientIP);

    req.on('close', function() {
        console.log("Client " + clientIP + " closed connection");
    })

    clientResponseDict[clientIP] = {}
    clientResponseDict[clientIP] = res;

    if (!pollingQueue.hasOwnProperty(clientIP)) {
        console.log("Polling queue does not have client IP " + clientIP + ", so launching one")
        pollingQueue[clientIP] = [];
    }

    else {
        // Check if there is something in the queue to send
        if (pollingQueue[clientIP].length > 0) {
            console.log("Polling queue has client IP " + clientIP + ", so sending response")
            res.end(JSON.stringify(pollingQueue[clientIP].shift()));
            clientResponseDict[clientIP] = {};
        }
    }
})

app.get('/api/initial', function(req, res) {
    console.log("API INITIAL REQUEST: " + JSON.stringify(tempState));
    
    const clientIP = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    
    pollingQueue[clientIP] = [];

    res.end(JSON.stringify(tempState));
})

function sendToAllClientsExceptSender(senderIP, data) {
    console.log("File to send:" + JSON.stringify(data));

    if (data.crdtName in tempState) {
        var tempFile = tempState[data.crdtName];
    }
    else {
        // create new local CRDT object depending on CRDT Type
        var tempFile = createLocalCRDT(data)
        tempFile.crdtType = data.crdtType;
    }

    tempState[data.crdtName] = tempFile.downstream(data.operation) // propagate operation to local CRDT object
    console.log("Temp State: " + JSON.stringify(tempState));

    // console log clientResponseDict keys
    //console.log("Client Response Dict: ")

    if (Object.keys(clientResponseDict).length === 0) {
        console.log("No clients to send to");
    }
    else {
        clients.forEach(function(client) {
            if (client !== senderIP) {
                pollingQueue[client].push(data);

                if (Object.keys(clientResponseDict[client]).length === 0) {
                    console.log("Can't use Long Polling for this client");
                }

                else {
                    clientResponseDict[client].end(JSON.stringify(pollingQueue[client].shift())); // send response to client and remove from queue
                    clientResponseDict[client] = {}; // reset response
                }
            }

            else {
                console.log("Client is sender: " + client);
            }
        })
    }

    console.log("Polling Queue: " + JSON.stringify(pollingQueue));

}

function createLocalCRDT(data) {
    switch (data.crdtType) {
        case "lwwRegister":
            return new crdt.OpLwwRegister(data.crdtName, false, data.operation.timestamp - 1);
        case "opCounter":
            return new crdt.OpCounter(data.crdtName);
        case "opORSet":
            return new crdt.OpORSet(data.crdtName);
        default:
            console.log("Unexpected CRDT type");
            break;
    }
}

function registerClient(clientIP) {
    console.log("Registering client: " + clientIP);
    clients.push(clientIP);
}

const dbOptions = {useNewUrlParser: true, useUnifiedTopology: true};
mongoose.connect(process.env.DB_URI, dbOptions)
.then(() => console.log('Connected to DB'))
.catch(err => console.log(err));


const port = process.env.PORT || 4000;

const server = app.listen(port, () => {
    console.log(`Server running on port ${port}`);
    console.log(ip.address() + ':' + server.address().port);
} );



