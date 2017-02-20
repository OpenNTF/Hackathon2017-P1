/*
 * This code should be pasted into a script block inside the applicable integration-server Connections community
 */

function showTranslateDialog(url) {
  dojo.empty("watsonTranslateResult");
  translateDialog.show();
  
  var translateUrl = dojo.byId("urlToTranslate");
  dojo.setAttr(translateUrl, "value", url);
}
function watsonTranslate(){
  dojo.xhrGet({
    // The URL of the request
    //url: "https://bookmarksplus.mybluemix.net/.darwino-app/translate",
    url: "https://apps.collabservintegration.com/files/muse-static/",
    headers: {
        'muse-bluemix': true
    },
    // Allow only 2 seconds for call TODO: figure out reasonable timeout
    
    form: dojo.byId("translateForm"),

    // The success callback with result from server
    load: function(result) {
      alert("success");
    },
    error: function(error) {
      dojo.byId("watsonTranslateResult").textContent = "An error occurred: " + error;
    }
  });
}
var style = document.createElement('style');
style.type = 'text/css';
//Connections hides the dialog top section from us
style.innerHTML = '.lotusui30dojo .dijitDialogTitleBar { display: block !important }';
document.getElementsByTagName('head')[0].appendChild(style);

require(["dojo/query", "dojo/dom-attr"], function(query){
  query("#bookmarksTableContainer .lconnfeedEntryRow").forEach(function(bookmarkRow){
    query("a.lconnfeedTitle", bookmarkRow).forEach(function(anchor){
      var url = dojo.attr(anchor, "href");
      var buttonString = '<button style="background: url(https://cdn.pbrd.co/images/AJN6FGoBG.png);height: 123px;width: 117px;" onclick="showTranslateDialog(\'' + url +'\')" />';
      var button = dojo.place(buttonString, bookmarkRow, "last");
    });
  });
});

require(["dijit/Dialog", "dojo/domReady!"], function(Dialog){
  translateDialog = new Dialog({
    title: "Translate Dialog",
    content: '<form onsubmit="return false;" id="translateForm"><input id="urlToTranslate" type="hidden" name="url" value=""><h4>Translate this link</h4><span>Translate To:</span><select name="lang" id="watsonLangSelect"><option value="ARABIC">Arabic</option><option value="FRENCH">French</option><option value="ITALIAN">Italian</option><option value="SPANISH">Spanish</option></select><button id="translateForm" onClick="watsonTranslate();">Send Form</button><div id="watsonTranslateResult"></div></form>',
    style: "width: 300px;background: white;"
  });
});