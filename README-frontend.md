# Bookmarks Plus (Client Component)

The Bookmarks Plus front-end developed from the Connect 2017 Hackathon is intended to allow users to run Watson services against their bookmarks.  

Currently we are only providing translation, but the intention is to provide more, including tonal analysis.

The front-end for this project is built directly on top of IBM Connections using functionality new to Connections - for more details, please see Andre Hagemeier’s session at Connect 2017.

A front-end could be built for this service on top of any bookmarks UI including custom implementation.

Code Flow
=========

When connections summary loads, dojo is used to identify every bookmark on the page and insert a translate button
This button triggers a small dialog to appear where the user selects their desired translation language
When the user clicks translate, the result is displayed in the dialog


TODOs
=====

- Current results are plain text - service does provide option to return translated content in HTML, but ran out of time to implement that in front-end
- Pull list of available languages directly from Watson - currently, a static list
- Add buttons to other bookmarks view including bookmarks detail page and important bookmarks widget
- Assorted styling improvements
- Addition of other Watson services such as tonal analysis