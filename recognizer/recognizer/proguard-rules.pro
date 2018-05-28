# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/cpqd/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# http://proguard.sourceforge.net/manual/attributes.html
#
# Annotations are already kept in proguard-android.txt.
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, EnclosingMethod

-keepparameternames

-keep public interface br.com.cpqd.asr.android.CPqDASR { *; }

-keep interface br.com.cpqd.asr.android.CPqDASR$CPqDASRListener { *; }

-keep public class br.com.cpqd.asr.android.CPqDASRFactory {
          public static br.com.cpqd.asr.android.CPqDASR create(android.content.Context, java.lang.String, java.lang.String);
      }
-keep, allowobfuscation class br.com.cpqd.asr.recognizer.AsrServerConnectionThread$AsrClientEndpoint {
                            public void onOpen(javax.websocket.Session, javax.websocket.EndpointConfig);
                            public void onClose(javax.websocket.Session, javax.websocket.CloseReason);
                            public void onError(javax.websocket.Session, java.lang.Throwable);
                            public void onMessage(javax.websocket.Session, br.com.cpqd.asr.recognizer.AsrMessage);
                        }

-keep, allowobfuscation public class br.com.cpqd.asr.recognizer.AsrServerConnectionThread$AsrMessageDecoder implements javax.websocket.Decoder$Binary

-keep, allowobfuscation public class br.com.cpqd.asr.recognizer.AsrServerConnectionThread$AsrMessageEncoder implements javax.websocket.Encoder$Binary
