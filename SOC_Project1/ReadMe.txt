ReadMe

SOC Project 1 Server

- running "sbt run 'port#'' doesn't work on one line for windows. You have to run "sbt" first. Then you would type "run 'port#'" when the console says sbt>.

- The db for the server starts as empty

- I implemented the server to handle new users. If you send the location for a new user, the first total distance will be 0 for that newly added user. Then it will comput distance normally. If you do a new trip with an already existing user, the code will get the last record saved for that user and then start computing the distance.  