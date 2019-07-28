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
 
 package com.google.samples.quickstart.signin;

public class user {
    public String u_name;
    public String mail;
    public double dist;
    public String UID;

    //for local db
    public user(String u_name, String mail, double dist,String uid){
        this.u_name=u_name;
        this.mail=mail;
        this.dist=dist;
        this.UID=uid;
    }
}
// end class user
