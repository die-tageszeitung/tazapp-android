var TAZAPI = (function() {

	var menuStatus = false;

	return (function() {

	    /** Android-Spezifisch  **/
	    function getAbsoluteResourcePath() {
	        return ANDROIDAPI.getAbsoluteResourcePath();
	    }

		/**
		 * pageReady(percentSeen, lastPage)
		 * 
		 * percentSeen : Dargestellte Seiten position : Position auf der Seite
		 * 
		 * Damit signalisiert das JavaScript, dass die Seite fertig gerendert
		 * und bereit zum Darstellen ist
		 */
		function pageReady(percentSeen, position) {
			//console.log("pageReady");
			// JsBridge.call( "articlePageReady", undefined, percentSeen,
			// position );
			ANDROIDAPI.pageReady(percentSeen, position);
		}

		/**
		 * startAnimation
		 * 
		 * Übergangs-Animation starten in/out
		 * 
		 * @param string
		 *            in_out "in"|"out" für rein oder raus
		 * @param string
		 *            direction "NS" | "SN" | "EW" | "WE" für von oben nach
		 *            unten | unten nach oben | rechts nach links | links nach
		 *            rechts
		 * 
		 */
		function startAnimation(in_out, direction) {
			console.log("startAnimation");
			// JsBridge.call( "articlePageStartAnimation", undefined, in_out,
			// direction );
		}

		/**
		 * openUrl
		 * 
		 * @param string
		 *            url Link
		 */
		function openUrl(url) {
			ANDROIDAPI.openUrl(url);
			//			console.log("openUrl");
			// window.location.href = url;
		}

		// Konfiguration

		/**
		 * onConfigurationChanged
		 * 
		 * JS Handler für Config-Änderungen in der APP Muss vom Javascript des
		 * ePubs zur Verfügung gestellt werden
		 * 
		 * @param string
		 *            name Name der Konfigurationsvariablen
		 * @param string
		 *            value Neuer Wert der Konfigurationsvariablen
		 * 
		 * @return bool ok
		 * 
		 */
		function onConfigurationChanged(name, value) {
			console.log("onConfigurationChanged");
			// throw 'abstrakte Methode onConfigurationChanged';
		}

		/**
		 * onShowDirectory
		 * 
		 * JS Handler wenn das Inhalts-Verzeichnis aufgerufen wird
		 * 
		 * @return bool ok
		 * 
		 */
		function onShowDirectory() {
			console.log("onShowDirectory");
			// throw 'abstrakte Methode onShowDirectory';
		}

		/**
		 * getConfiguration
		 * 
		 * Konfigurationsvariablen
		 * 
		 * @param string
		 *            name
		 * 
		 * @return string value oder false, wenn es ihn nicht gibt oder Arbeiten
		 *         wir lieber mit Exeptions?
		 */
		function getConfiguration(name, callback) {
			callback(ANDROIDAPI.getConfiguration(name));
		}

		/**
		 * setConfiguration
		 * 
		 * @param string
		 *            name Name der Konfigurationsvariablen
		 * @param string
		 *            value Neuer Wert der Konfigurationsvariablen
		 * 
		 * return bool ok
		 */
		function setConfiguration(name, value) {
			if (typeof (value) != 'string')
				value = value.toString();
			return ANDROIDAPI.setConfiguration(name, value);
		}

		// Values

		/**
		 * getValue
		 * 
		 * Wert setzen Name/Werte-Paare wobei der Name ein Pfad ist, der bei
		 * Daten zu einem ePub immer mit /bookId beginnt
		 * 
		 * @param string
		 *            path
		 * 
		 * @return string value oder false, wenn es ihn nicht gibt oder Arbeiten
		 *         wir dann mit Exeptions?
		 */
		function getValue(path, callback) {
			callback(ANDROIDAPI.getValue(path));
		}

		/**
		 * setValue
		 * 
		 * Wert setzen
		 * 
		 * @param string
		 *            path
		 * @param string
		 *            value
		 * 
		 * @return bool ok
		 */
		function setValue(path, value) {
			return ANDROIDAPI.setValue(path, value);
		}

		/**
		 * removeValue
		 * 
		 * Wert löschen
		 * 
		 * @param path
		 * 
		 * @return bool ok
		 */
		function removeValue(path) {
			console.log("removeValue");
			// JsBridge.call( "articleRemoveValue", undefined, path );
		}

		/**
		 * removeValueDir
		 * 
		 * Baumbereich löschen ??? Alle Values, deren path mit 'path' beginnt
		 * ???
		 * 
		 * @param path
		 * 
		 * @return bool ok
		 */
		function removeValueDir(path) {
			console.log("removeValueDir");
			// JsBridge.call( "articleRemoveValueDir", undefined, path );
		}

		/**
		 * getBookmarks(callback)
		 * 
		 */
		function getBookmarks(callback) {
			console.log("getBookmarks");
			// JsBridge.call( "articleGetBookmarks", callback );
		}

		/**
		 * setBookmark(artname,isBookmarked)
		 * 
		 */
		function setBookmark(artname, isBookmarked) {
			console.log("setBookmark");
			// JsBridge.call( "articleSetBookmark", undefined, artname,
			// isBookmarked );
		}

		/**
		 * listKeys
		 * 
		 * Array mit allen Keys mit Value
		 * 
		 * ??????????????????????????????
		 * 
		 * @param path
		 * 
		 * @return array oder false? bzw. Exeption
		 */
		function listKeys(path) {
			console.log("listKeys");
			EPUBTAZ.log("TAZAPI.listKeys: path=", path);
			var ret;
			for ( var name in localStorage) {
				if (name.test(',^' + path + '/.+/'))
					ret.name = localStorage[name];
			}
			return false;
		}

		// Vergessene Methoden

		/**
		 * clearWebCache
		 * 
		 * Löscht den Cache von Webkit. gerade bei dem Debugging ist es gut den
		 * Cache zu löschen
		 * 
		 */
		function clearWebCache() {
			console.log("clearWebCache");
			// JsBridge.call( "articleClearWebCache", undefined );
		}

		/**
		 * netLog
		 * 
		 * zum Debugging, am schicksten wäre es wenn im Hintergrund einfach die
		 * URL geöffnet wird Beim Test mache ich das jetzt mit einer
		 * Ajax-Funktion, so kann ich auf dem Server sehen was passiert. Es
		 * werden keine Daten zurück geliefert. Siehe setupApp.js
		 * 
		 * @param string
		 *            url URL
		 * 
		 */
		function netLog(str) {
			console.log("netLog");
			// JsBridge.log( str );
		}

		function statechangeHandler() { // macht nichts
			console.log("statechangeHandler");
			return true;
		}

		/**
		 * menuStatusCallback
		 * 
		 * wird von Objective-C gerufen, wenn sich der Menue-Status ändert.
		 */
		function menuStatusCallback(currentStatus) {
			console.log("menuStatusCallback");
			// menuStatus = currentStatus;
		}

		/**
		 * getMenu
		 * 
		 * Status vom Menü bzw. ActionBar
		 * 
		 * @return bool status sichbar oder nicht sichtbar
		 * 
		 */
		function getMenu() {
			console.log("getMenu");
			// return menuStatus;
		}

		/**
		 * setMenu
		 * 
		 * Status vom Menü bzw. ActionBar
		 * 
		 * @param bool
		 *            status sichbar oder nicht sichtbar
		 * 
		 */
		function setMenu(status) {
			console.log("setMenu");
			// JsBridge.call( "articleSetMenu", undefined, status );
		}

		function enableRegionScroll(isOn) {
			//console.log("enableRegionScroll");
			ANDROIDAPI.enableRegionScroll(isOn);
			// JsBridge.call( "articleEnableRegionScroll", undefined, isOn );
		}

		function nextArticle(position) {
			//console.log("nextArticle " + position);
			ANDROIDAPI.nextArticle(position);
			// JsBridge.call( "articleNext", undefined, position );
		}

		function previousArticle(position) {
			//console.log("previousArticle");
			ANDROIDAPI.previousArticle(position);
			// JsBridge.call( "articlePrevious", undefined, position );
		}

		function beginRendering() {
			//	console.log("beginRendering");
			ANDROIDAPI.beginRendering();
			// JsBridge.call( "articleBeginRendering", undefined );
		}

		return {
		    getAbsoluteResourcePath : getAbsoluteResourcePath,
			pageReady : pageReady,
			startAnimation : startAnimation,
			openUrl : openUrl,
			onConfigurationChanged : onConfigurationChanged,
			getConfiguration : getConfiguration,
			setConfiguration : setConfiguration,
			getValue : getValue,
			setValue : setValue,
			removeValue : removeValue,
			removeValueDir : removeValueDir,
			listKeys : listKeys,
			clearWebCache : clearWebCache,
			netLog : netLog,
			menuStatusCallback : menuStatusCallback,
			getMenu : getMenu,
			setMenu : setMenu,
			getBookmarks : getBookmarks,
			setBookmark : setBookmark,
			enableRegionScroll : enableRegionScroll,
			nextArticle : nextArticle,
			previousArticle : previousArticle,
			beginRendering : beginRendering
		}
	}());
}());
