# Sentinel


# Sentinel Android application to observe and report change in image or video stream.
The source is in the Sentinel directory.<br>

The application makes use of The Catalano Framework:
<a href="https://code.google.com/archive/p/catalano-framework">https://code.google.com/archive/p/catalano-framework/</a><br>
which is under LGPL license:<a href="http://www.gnu.org/licenses/lgpl.html">http://www.gnu.org/licenses/lgpl.html</a><br>

<br>
Sound from www.soundjay.com:<br>
<a href="https://www.soundjay.com/tos.html">https://www.soundjay.com/tos.html</a><br>

<br>
Description:<br>
<a href="http://techzealous.blogspot.bg/2017/01/object-movement-software.html">http://techzealous.blogspot.bg/2017/01/object-movement-software.html</a><br>
<a href="http://techzealous.blogspot.com/2018/03/sentinel-image-capture-software.html">http://techzealous.blogspot.com/2018/03/sentinel-image-capture-software.html</a><br>


# Sentinel Java application
Uses the computer's web camera to record images or videos when there is detected movement in the video.<br>

The source is in the Sentinel_WebCamera directory.<br>
To build the project you need to download the libraries from the links below and to copy them to the
project's libs directory.
<br>
<a href="http://central.maven.org/maven2/com/github/sarxos/webcam-capture/0.3.10/">http://central.maven.org/maven2/com/github/sarxos/webcam-capture/0.3.10/</a>
<br>
(<a href="http://central.maven.org/maven2/com/github/sarxos/webcam-capture/0.3.10/webcam-capture-0.3.10.jar">webcam-capture-0.3.10.jar</a>)
<br>
<br>
<a href="http://central.maven.org/maven2/com/nativelibs4java/bridj/0.6.2/">http://central.maven.org/maven2/com/nativelibs4java/bridj/0.6.2/</a>
<br>
(<a href="http://central.maven.org/maven2/com/nativelibs4java/bridj/0.6.2/bridj-0.6.2.jar">bridj-0.6.2.jar</ar>)
<br>
<br>
<a href="http://maven.aliyun.com/nexus/content/repositories/releases/org/slf4j/slf4j-api/1.7.20/">http://maven.aliyun.com/nexus/content/repositories/releases/org/slf4j/slf4j-api/1.7.20/</a>
<br>
(<a href="http://maven.aliyun.com/nexus/content/repositories/releases/org/slf4j/slf4j-api/1.7.20/slf4j-api-1.7.20.jar">slf4j-api-1.7.20.jar</ar>)
<br>
<br>
<a href="https://www.dcm4che.org/maven2/xuggle/xuggle-xuggler/5.4/">https://www.dcm4che.org/maven2/xuggle/xuggle-xuggler/5.4/</a>
<br>
(<a href="https://www.dcm4che.org/maven2/xuggle/xuggle-xuggler/5.4/xuggle-xuggler-5.4.jar">xuggle-xuggler-5.4.jar</a>)

<br>
<br>
<br>

Downloads:<br>

<a href="https://github.com/ektodorov/sentinel/blob/master/Downloads/Sentinel.zip">Sentinel WebCamera for Windows, Linux, macOS</a>
<br>
- option to select video or images<br>
- the images or videos will be recorded in the current working directory of the application.<br>
<br>

Supported OS - Windows, Linux, macOS.<br>
You need Java runtime installed on the system to run the application.<br>
<br>
Run:<br>
Windows and Linux<br>
 - by double clicking on the Sentinel.jar<br>
 - or from the command line / temrinal by typing:<br>
<br>
java -jar Sentinel.jar
<br>
<br>
macOS you have to run from the terminal.<br>
1.Applications -> Terminal<br>
2.Go to the directory where you unarchied the Sentinel.zip and type<br>
<br>
java -jar Sentinel.jar<br>
<br>
3.Give permission to use the camera<br>
4.When you want to close select the window of the Terminal.app and press Ctrl+C<br>
