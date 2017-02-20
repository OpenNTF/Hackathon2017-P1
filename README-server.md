# Bookmarks Plus (Server Component)

The server component of Bookmarks Plus is a [Darwino](http://darwino.com) application that can run on a generic Java web server or Bluemix.

To build the application, you will need access to Darwino (the free Community Edition will work) and have it configured for Maven use. Those instructions can be found in the documentation on [the Darwino Playground](http://playground.darwino.com).

To run the application, either build the project tree and deploy the war file in `bookmarks-plus-j2ee` or import the projects into Eclipse and run that project on the server using an embedded server.

Configuring Watson Access
=========================

The application requires access to an active Watson Language Translation service. Once that is set up, the credentials and URL should be specified in an active Darwino properties file. It should contain properties like:

	bookmarksplus.translation.user=some-user
	bookmarksplus.translation.password=some-password

In a non-Bluemix environment, this file can be in the root of the web server or in the ".darwino" directory in the running user's home.

For a Bluemix environment, create a file named "darwino.bluemix.properties" in the `src/main/webapp/WEB-INF` folder in the `bookmarks-plus-j2ee` project and specify them there. This will be uploaded to Bluemix, but is .gitignore'd for security.

Database Access on Bluemix
==========================

When running on Bluemix, the app expects there to be an available instance of Compose PostgreSQL. When that is set up, add these properties to the Bluemix Darwino properties file:

	bookmarksplus.db.url=jdbc:postgresql://somehost:12345/compose
	bookmarksplus.db.db=postgresql
	bookmarksplus.db.user=admin
	bookmarksplus.db.password=SOMEPASSWORD

Note that the JDBC URL is slightly different from the URL given in the service properties.

Translation Service Parameters
==============================

When deployed, the translation service is available via `/appname/.darwino-app/translate` and supports HTTP Basic authentication. It has several parameters:

- url (required): the full URL to translate, including the http:// or https://
- lang (required): the destination language. One of ARABIC, ENGLISH, SPANISH , FRENCH, ITALIAN, PORTUGUESE
- type: specify "html" for a translated version of the original HTML. Otherwise, it is only the text
- direct: specify "true" to have the service return the text or HTML as the response directly. Otherwise, it responds with a JSON object with the result in the "result" key