## Learning objectives
- Familiarity with Android development.
- Familiarity with Play framework, including server setup and database access.
- Streaming mechanism such as Websocket.

## Story

We are implementing an Android application that tracks a user’s location during outdoor
activities (walking, running, or cycling). The application constantly sends the user’s location to a
server, which saves the location information in a database and tells the user how far he or she
has gone.

## Main deliverables
- An Android application that keeps sending its location as a JSON object to a server and
showing the total distance returned by the server.
  - Submit both of the source code and the APK file. Place the apk file at the root
directory so it can be easily found.
  - There should be two text input boxes and one button within the UI. One text input box is for host name of the server, its default value should be
10.0.2.2:9000 , which is the host machine address for android emulator; another text input box is for user name. The button is for starting/stopping. Refer to the screenshot at the last page as an example.
- A server that accepts requests in JSON format, saves the location information in an SQL
database, and sends the total distance back to the client.
  - Submit the whole project, including the sbt build script( build.sbt ). Make sure the
server can be started by executing command ‘ sbt run ’ in your submitted directory.

## Ingredients
- JDK 8 or higher (required)
- sbt (ver 1.2.1 or higher) [tested with ver 1.2.8]
- Play (ver 2.6.18 or higher) [Don’t need to download manually, installed with sbt]
- Android Studio (ver 3.1.4 or higher) [tested with ver 3.5]
- (optional library) Volley (1.1.1 or higher)
- (optional library) Tyrus Standalone Client (1.9 or higher)

## Guidelines
### Server
#### 1. Initialization
  - Install the latest version of sbt : http://www.scala-sbt.org/download.html
  - Create a new Play project with: ```sbt new playframework/play-java-seed.g8``` (Enter project name and other info as prompted)
  - Try running the server from the project root: sbt run (The first run will take some time as dependencies are downloaded. Test it by visiting localhost:9000.) Refer to: https://www.playframework.com/documentation/2.7.x/NewApplication
  - (Optional) Setup IDE for your Play application. Refer to: https://www.playframework.com/documentation/2.7.x/IDE
#### 2. Adding a POST Method
  - Add a route in conf/routes like this: POST /locationupdate controllers.HomeController.handleupdates
  - Add a method in your home controller to handle this request. In app/controllers/HomeController.java , add handleupdates() method.
    - Hint: You can use DynamicForm and FormFactory to handle POST requests:
      ```
      ...
      import com.google.inject.Inject;
      import play.data.DynamicForm;
      import play.data.FormFactory;
      …
      @Inject
      FormFactory formFactory;
      public Result handleupdates() {
      DynamicForm dynamicForm = formFactory.form().bindFromRequest();
      //Logger.info("Username is: " + dynamicForm.get("username"));
      //Logger.info("Time is: " + dynamicForm.get("timestamp"));
      //Logger.info("Latitude is: " + dynamicForm.get("latitude"));
      //Logger.info("Longitude is: " + dynamicForm.get("longitude"));
      return ok("ok, I received POST data. That's all…\n");
      }
      ```
  - Handling and Serving JSON. Refer to: https://playframework.com/documentation/2.7.x/JavaJsonActions. Return operation information in JSON format.
    Hint
    ● Import jackson.databind.JsonNode and jackson.databind.node.ObjectNode .
#### 3. SQL Database Access
- Add a method to store users’ location tracking information in an SQL database, use SQLite as your database engine. Refer to: https://www.playframework.com/documentation/2.7.x/JavaDatabase and http://www.sqlitetutorial.net/sqlite-java/
    - Notes
      - Configure database properties in conf/application.conf file.
      - Inject a Play.db.Database to use the default datasource.
      - (Optional) Use asynchronous action for database operations.
- Calculate the movement of the user since start, return it in the response to POST request.
#### 4. Running the Server
- Use: sbt run to run this application. The default port for a Play application is 9000. You can access your server from: localhost:9000
- If you wish to access your application via other host names, add them in conf/application.conf like this:
    ````
    play.filters.hosts {
    allowed = ["localhost:9001", "example.com:9000", "localhost:9000"]
    }
    ````
    - You can use:sbt run 9001 to run this application from port 9001.

### Android App
- Install Android Studio. Use the latest version: https://developer.android.com/studio/index.html
- Create a new Android project.
    - Notes
      - Minimum SDK: API 15: Android 4.0.3 (IceCreamSandwich).
      - Add internet ( android.permission.INTERNET ) and location ( android.permission.ACCESS_FINE_LOCATION ) permissions in AndroidManifest.xml .
      - Add ‘android:usesCleartextTraffic="true"’ to the <application/> tag in your AndroidManifest.xml, if you are targeting API 23 or higher.
      - Add an EditText for user name input and at least one Button for basic operations.
    - Hint
      - You may want to check whether your application actually has location permission upon startup. (Use ContextCompat.checkSelfPermission )
- Add a method for POST requests.
    - Notes
      - Use Volley. Refer to: https://developer.android.com/training/volley/index.html
      - POST at least four parameters: username , timestamp , latitude , and longitude .
      - When the Button is clicked, start a thread that makes a POST request every 5 seconds. Click Button to stop the thread.
      - Display the message returned from server.
    - Hint
      - If you are using an Android Emulator, use your IP address for Server URL instead of “localhost”.
- Use JSON.
    -Use Volley’s JsonObjectRequest for JSON post requests.
    - Example JSON request:
    ```
        header "Content-type: application/json"
        request POST
        data '{
        "username": "GUEST",
        "timestamp": "1501745843691",
        "latitude": "47.7474",
        "longitude": "15.7694"
        }'
     ```
    - You may add any other fields to make your application better.
- Alternative: use Play’s WebSocket, and stream location information to server. Refer to: https://www.playframework.com/documentation/2.7.x/JavaWebSockets
  - Hint
    - You can use the Tyrus library for WebSocket programming on the Android side. Refer to: https://github.com/tyrus-project/tyrus
    - Use WebSocket.json(JsonNode.class).accept instead of WebSocket.Json.accept on thE server side. On the Android side, send JsonNode object instead of Text.
    - Include Jackson’s core, databind, and annotation libraries to avoid errors.

## Grading Rubric

### Submission: 10
- Compiled APK: 2
- No absolute paths: 2
- Server source: 3
- Client source: 3


### Client: 45
- App starts: 5
- UI: 10
- Default host: 2
- Editable host: 2
- Editable username: 3
- Start (and stop) button: 3
- Functionality: 30
- Updates shown realtime (after every request): 5
- Start (and stop) button works (starts and stops update: 5
- Sends location update periodically (approx. 5 sec): 10
- Shows received total distance correctly: 10

### Server: 45
- Server starts: 5
- Receives and parses requests correctly (indicated by debug output): 10
- Values stored in sqlite db: 5
- Distance persists across restarts: 5
- Per-username calculations: 10
- Reasonable distance calculation: 10
