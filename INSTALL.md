Bauen und Installieren der taz.app
==================================

Die taz.app wird mit Googles *Android Studio* gebaut. Sofern noch nicht 
installiert, kann *Android Studio* von 
  [developer.android.com](http://developer.android.com/develop/index.html)
bezogen werden (dem Link *Set up Android Studio* folgen). *Android Studio* 
setzt ein JDK
([oracle.com](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)) voraus.
Der Setup Wizard lädt auch gleich ein ein Android SDK und andere Dinge von
Googles Servern nach.

Im *Welcome*-Screen klickt man *Check out from Version Control -> Git* an
und gibt dort
  [https://github.com/die-tageszeitung/tazapp-android.git](https://github.com/die-tageszeitung/tazapp-android.git)
ein. Anschliessend muss noch die Frage, ob das Project von build.gradle 
importiert werden soll, mit OK beantwortet werden und nach kurzer Wartezeit
öffnet sich das Android Studio Hauptfenster.

Auf dem Android-Gerät sollte zunächst die dort installierte releaste taz.app
deinstalliert und - sofern noch nicht geschehen - der Entwicklermodus
und USB-Debugging eingeschaltet werden.

Über *Build -> Make Project* und *Run -> Run 'app'* wird die App auf dem 
Gerät installiert und ausgeführt.
