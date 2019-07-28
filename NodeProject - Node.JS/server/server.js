/*
================================================================================

Walkerz server

authors:
victor martinov
yael lustig
all rights ressrved =]
17/9/2018

================================================================================
 */


var express = require('express'),
    bodyParser = require('body-parser'),
    methodOverride = require('method-override'),
    test = require('./routes/test'),
    users = require('./routes/users'),
    app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
    extended: true
}));
app.use(methodOverride());      // simulate DELETE and PUT

// CORS (Cross-Origin Resource Sharing) headers to support Cross-site HTTP requests
app.all('*', function (req, res, next) {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Headers", "X-Requested-With");
    next();
});


//upon receival of requests, redirect to these functions.

app.get('/users/comp', users.competition); // get competition data
app.post('/users/GPScoordinatesForUser', users.UserGPScoord); // get gps coordinates for specific user
app.get('/top5', users.top5); //get top5 walkers from remote db
app.post('/newU', users.newU); //post new user to remote db
app.post('/update', users.GPS_update); // update gps points to db
app.post('/update_dist', users.GPS_update_dist);
app.get('/y_top5', users.y_top5); //get yesterdays top5 walkers from remote db


//set listeening port, and start listening.
app.set('port', 5000);
app.listen(app.get('port'), function () {
    console.log('vicYael server listening on port ' + app.get('port'));
});


//set daily top5 update email time

var now = new Date();
var millisTill10 = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 20, 00, 0, 0) - now; //set email to 20:00 daily
if (millisTill10 < 0) {
    millisTill10 += 86400000; // it's after 20:00, try 20:00 tomorrow.
}

//daily top5 email to users
setTimeout(function () {
        var mysql = require('mysql');
        var con = mysql.createConnection({
            host: "sql7.freesqldatabase.com",
            user: "userID",
            password: "userPass",
            database: "userDB"
        });
        con.connect(function (err) { //connect to sql server
            if (err) throw err;

            //get today's top5 walkerz from remote db
            con.query("SELECT u_name as 'name',u_dist as 'distance' FROM Users ORDER BY u_dist DESC LIMIT 5", function (err, res, fields) {
                top5 = JSON.parse(JSON.stringify(res));
                if (err)
                    throw err;
                //console.log(top5);

                //send daily emails to ALL users!
                con.query("SELECT u_mail as 'email' FROM Users", function (err, result, fields) { //get all emails
                    //var emails = JSON.parse(JSON.stringify( result )); for debug
                    var nodemailer = require('nodemailer');

                    var transporter = nodemailer.createTransport({
                        service: 'gmail',
                        auth: {
                            user: 'user@gmail.com',
                            pass: 'userPass'
                        }
                    });

                    //send individual emails
                    for (var i = 0; i < result.length; i++) {

                        var mailOptions = {
                            from: 'from@gmail.com',
                            to: result[i].email, //email address
                            subject: 'todays top5 walkerz!',
                            text: JSON.stringify(top5) //the top5 text, hopefully
                        };

                        transporter.sendMail(mailOptions, function (error, info) {
                            if (error) {
                                console.log(error);
                            }
                            else {
                                console.log('Email sent: ' + info.response);
                            }
                        });
                    }
                    if (err)
                        throw err;

                    //delete yesterdays top5
                    con.query("DELETE FROM yesterdays_top5", function (err, res, fields) {
                        if (err)
                            throw err;
                        console.log('deleted yesterdays top5');
                    });

                    //move today's top5 to yesterdays top5
                    con.query("INSERT INTO yesterdays_top5 SELECT * FROM Users ORDER BY u_dist DESC LIMIT 5", function (err, res, fields) {
                        if (err)
                            throw err;
                        console.log('moved todays top5 to yesterdays top5');
                    });

                    //reset all distances.
                    con.query("UPDATE Users SET u_dist = 0", function (err, res, fields) {
                        if (err)
                            throw err;
                        console.log('dist reset for all users');
                    });

                    //delete gps table records
                    con.query("DELETE FROM GPS_coord", function (err, res, fields) {
                        // for older than 24hrs: con.query("DELETE FROM GPS_coord WHERE update_time < (NOW() - INTERVAL 24 HOUR)", function (err, res, fields) {
                        if (err)
                            throw err;
                        console.log('deleted all gps records'); //todo older than 24hrs or all current gps records?
                    });
                    con.end();
                });
            });
        });
    }, millisTill10
);


/*
//could also use for specific reminder
    var remind = require('email-reminder');
    var email_sender = 'email_sender@gmail.com';
    var email_recipient = 'email_recipient@gmail.com';
    var rem_at = remind.at('15:23:00', email_sender, email_recipient, 'Get ready for team lunch'); //provide time in 24 hour format*!/
    */