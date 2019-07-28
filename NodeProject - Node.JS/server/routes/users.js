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

//insert new user details to sql database
exports.newU = function (req, res) {

    //insert new user into sql db
    //get db
    var mysql = require('mysql');
    var con = mysql.createConnection({
        host: "sql7.freesqldatabase.com",
        user: "userID",
        password: "userPass",
        database: "userDB"
    });

    // parse application/json
    var express = require('express')
    var bodyParser = require('body-parser');
    var app = express();
    app.use(bodyParser.json());

    //console.log(req.body); // not a string, but your parsed JSON data

    //connect to db and insert data
    con.connect(function (err) {
        if (err)
            throw err;
        var mom = require('moment');

        //DON'T INSERT if UID exists in table.
        var sql = "INSERT IGNORE INTO Users (u_name, u_mail,u_ID,u_dist,last_update) VALUES ('" + req.body.username + "','" + req.body.e_mail + "','" + req.body.person_id + "','" + req.body.dist + "','" + mom(Date.now()).format('YYYY-MM-DD HH:mm:ss') + "')";
        con.query(sql, function (err, result) {
            if (err) {
                throw err;
            }
            //upon success
            console.log("record inserted in SQL db for " + req.body.username + "\n(if duplicate uid than not inserted..)");
            con.end(); //close connection to remote db
        });
    });
};
//end newU


//returns the top 5 walkers of today
exports.top5 = function (req, res, next) {
    var mysql = require('mysql');
    var con = mysql.createConnection({
        host: "sql7.freesqldatabase.com",
        user: "userID",
        password: "userPass",
        database: "userDB"
    });

    //connect to sql server and exequte proper query
    con.connect(function (err) {
        if (err) throw err;
        //get top5 walkerz
        con.query("SELECT * FROM Users ORDER BY u_dist DESC LIMIT 5", function (err, result, fields) {
            //SELECT u_name as 'name',u_dist as 'distance' FROM Users ORDER BY u_dist DESC LIMIT 5
            res.send(result);
            if (err)
                throw err;
            console.log("top5"); //upon success
            con.end(); //close connection to remote db
        });
    });
};
//end top5


//returns yesterdays top 5 walkers
exports.y_top5 = function (req, res, next) {
    var mysql = require('mysql');
    var con = mysql.createConnection({
        host: "sql7.freesqldatabase.com",
        user: "userID",
        password: "userPass",
        database: "userDB"
    });

    //connect to sql server and get yesterdays top5 walkerz
    con.connect(function (err) {
        if (err) throw err;
        con.query("SELECT * FROM yesterdays_top5 ORDER BY u_dist DESC LIMIT 5", function (err, result, fields) {
            //SELECT u_name as 'name',u_dist as 'distance' FROM yesterdays_top5 ORDER BY u_dist DESC LIMIT 5
            res.send(result);
            //console.log(result);
            if (err)
                throw err;
            console.log("y_top5"); //upon success
            con.end(); //close connection to remote db
        });
    });
};
//end y_top5


//get competition details from sql database
exports.competition = function (req, res, next) {
    var mysql = require('mysql');
    var con = mysql.createConnection({
        host: "sql7.freesqldatabase.com",
        user: "userID",
        password: "userPass",
        database: "userDB"
    });

    //connect to db and get competition real time data
    con.connect(function (err) {
        if (err)
            throw err;
        //console.log("get qurey from competition");
        var sql = "SELECT * FROM Users ORDER BY u_dist DESC";
        con.query(sql, function (err, result, fields) {
            if (err) {
                throw err;
            }
            res.send(result);
            console.log("competition"); //upon success
        });
        con.end(); //close connection to remote db
    });
};
//end competition


//update gps coordinates in db
exports.GPS_update = function (req, res, next) {

    var mysql = require('mysql');
    var connection = mysql.createConnection({
        host: "sql7.freesqldatabase.com",
        user: "userID",
        password: "userPass",
        database: "userDB"
    });

    //Parse data from JSON POST and insert into MYSQL
    var express = require('express');
    var app = express();
    var bodyParser = require('body-parser');

    //connect to sql server and exequte proper query
    connection.connect(function (err) {
        if (err)
            throw err
/*        else {
            Start the app when connection is ready
            app.listen(3000);
            console.log('Server listening on port 3000');
        }*/
    });
    app.use(bodyParser.json())

    var jsondata = req.body;
    var values = [];

    var mom = require('moment');

    //copy json data to array to send
    for (var i = 0; i < jsondata.length; i++)
        values.push([jsondata[i].U_google_ID, jsondata[i].Lattitude, jsondata[i].Longtitude, mom(Date.now()).format('YYYY-MM-DD HH:mm:ss')]);

    //Bulk insert using nested array [ [a,b],[c,d] ] will be flattened to (a,b),(c,d)
    connection.query('INSERT INTO GPS_coord (userID, latitude,longtitude,update_time) VALUES ?', [values], function (err, result) {
        if (err) {
            res.send('Error');
        }
        else {
            res.send("gps points updated");
            console.log("GPS coordinates updated."); //upon success
        }
        connection.end(); //close connection to remote db
    });
};
//end GPS_update


//update user GPS coordinates details to sql database
exports.GPS_update_dist = function (req, res) {

    //insert new user into sql db
    //get db
    var mysql = require('mysql');
    var con = mysql.createConnection({
        host: "sql7.freesqldatabase.com",
        user: "userID",
        password: "userPass",
        database: "userDB"
    });

    // parse application/json
    var express = require('express')
    var bodyParser = require('body-parser');
    var app = express();
    app.use(bodyParser.json());

    //connect to db and insert data
    var dist = req.body.dist;
    var uid = req.body.uid;

    //connect to sql server and update gps coordinates for current user.
    con.connect(function (err) {
        if (err)
            throw err;

        //update user distance
        con.query("UPDATE Users SET u_dist = '" + dist + "' WHERE  u_ID =  '" + uid + "'", function (err, result) {
            if (err) {
                throw err;
            }
            console.log("gps update dist"); //upon success
            con.end(); //close connection to remote db
        });
    });
};
//end GPS_update_dist

//get user's gps coordinates details from sql database
exports.UserGPScoord = function (req, res, next) {
    var mysql = require('mysql');
    var con = mysql.createConnection({
        host: "sql7.freesqldatabase.com",
        user: "userID",
        password: "userPass",
        database: "userDB"
    });

    //connect to db and get user's gps coordinates
    con.connect(function (err) {
        if (err)
            throw err;
        var sql = "SELECT latitude,longtitude FROM GPS_coord where userID = '" + req.body.userID + "'";
        //SELECT u_name as 'name',u_dist as 'distance' FROM Users ORDER BY u_dist DESC LIMIT 5
        con.query(sql, function (err, result, fields) {
            if (err) {
                throw err;
            }
            res.send(result);
            console.log("gps points sent to user with id: " + req.body.userID); //upon success
        });
        con.end(); //close connection to remote db
    });
};
//end competition


//function to send specific email to specific user
exports.sendEmail = function (req, res, next) {

    var nodemailer = require('nodemailer');

    var sendFrom = 'sendFrom@gmail.com';
    var pass = 'sendFromPass';
    var sendTo = 'sendTo@gmail.com';

    var transporter = nodemailer.createTransport({
        service: 'gmail',
        auth: {
            user: sendFrom,
            pass: pass
        }
    });

    var mailOptions = {
        from: sendFrom,
        to: sendTo,
        subject: 'Email from me',
        text: 'hi'
    };

    transporter.sendMail(mailOptions, function (error, info) {
        if (error) {
            console.log(error);
        } else {
            console.log('Email sent: ' + info.response);
        }
    });
};
//end sendEmail - unused.. different method in use.







/*
//for debug

var users = [
    {id:0 , name:"Shay"},
    {id:1 , name:"Ram"},
    {id:2 , name:"Shlomi"}
];

exports.findAll = function (req, res, next) {
    res.send(users);
    console.log('findAll:');
};

exports.findById = function (req, res, next) {
    var id = req.params.id;
    res.send(users[id]);
    console.log('findById:');
};*/