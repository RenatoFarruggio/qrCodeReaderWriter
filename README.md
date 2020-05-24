# ~~soundLink~~ -> qrLink

## Content
* [Contributors](#contributors)
* [Idea](#idea)
* [API](#api)
* [Execution](#execution)
* [Python-Example](#python-example)
* [Compatibility](#compatibility)
* [Diary](#diary)
* [Todo](#todo)

## Contributors
* Renato Farruggio
* Caroline Steiblin

## Idea
Implement an interface that allows two Android phones to send and receive QR codes between each other, using [Zxing](https://github.com/zxing/zxing) library. Synchronize any database from BACnet from an Android device to another Android device over qr codes.

## API
We offer the following API to group 12 (logSync) for sending and receiving messages over qr:
```java
   public byte[] rd_callback() // called when logSync wants to receive
   public int wr_callback(byte[]) // called when logSync wants to send
```
The usage should be similar to send() and recv() from UDP.  
In return, we will need a method to start logSync:
```python
   startLogSync(rdcb, wrcb);
```

## Execution
Sequence of control will be as follows:
* We start logSync server as part of the logic behind the UI:
```java
   startLogSync(rd_callback, wr_callback);
```
Control is then handed over to the python application (logSync).
* logSync calls callback to receive or to send.  
* logSync retains control until sync is completed.

## Python-Example
Code on python side should look like this:  
```python
   def startLogSync(rdcb, wrcb):
     t = threading.Thread(Log_Sync_Thread, args=(rdcb,wrcb))
     t.start()
   
   class Log_Sync_Thread:
     def __init__(self):
       pass
       
     def run(rdcb, wrcb):
       self.recv = rdcb
       self.send = wrcb
       while True:
         ... # Main loop
```
## Compatibility
Packet format compatible with log requirements [here](https://github.com/cn-uofbasel/BACnet/blob/master/doc/BACnet-event-structure.md). But we can send any bytes that standard UDP packets can send. We have, however, adapted our code to fit the transport interface

## Diary
All meeting notes are located in the diary, [here](https://github.com/cn-uofbasel/BACnet/blob/master/groups/02-soundLink/Tagebuch.md).

## TODO:
* ~~Write proper README~~
* ~~Access front camera~~
* ~~Implement [asking user for permission to use camera](https://github.com/ParkSangGwon/TedPermission)~~
* ~~Test if QR code gets recognized through front camera (It does!)~~
* ~~Implement beep sound as feedback for qr code recognition~~
* ~~Disable screen rotation~~
* ~~Implement [Android Image Dialog/Popup](https://stackoverflow.com/questions/7693633/android-image-dialog-popup) and test if it works as expected.~~
* ~~In case, above Image Dialog does not work, find another way to display qr code while having the camera open.~~
* ~~Implement dialog over qr code~~
* ~~Make qr code size adaptive to screen size (maximal but not bigger than screen)~~
* ~~Implement packet splitting (doesn't seem like we need that)~~
* ~~Find out how many bps we can send (around 500bytes/packet, maybe up to 2k) should be fine !~~
* ~~Add [Chaquopy](https://chaquo.com/chaquopy/) to our project to run python files~~
* ~~Fix __fatal__ bug: Chaquopy can't import python library "cbor2" (Fix by downgrading to cbor)~~
* ~~__Encode and Decode qr code in base64 encoding__ (Does not work with our current setup afaik)~~
* ~~If Chaquopy works as exspected, add it to the documentation over on [BACnet](https://github.com/cn-uofbasel/BACnet/tree/master/groups/02-soundLink)~~
* ~~Merge readme from [BACnet](https://github.com/cn-uofbasel/BACnet/tree/master/groups/02-soundLink) into this readme.~~
* ~~Figure out how to use the [eventCreationTool](https://github.com/cn-uofbasel/BACnet/tree/master/groups/04-logMerge/eventCreationTool) on our android app, according to [this](https://chaquo.com/chaquopy/doc/current/java.html)~~
* ~~Make a license file (preferably the same, BACnet uses)~~
* ~~Get a [license for Chaquopy](https://chaquo.com/chaquopy/license/?app=ch.unibas.qrscanner)~~
* ~~Specify Interface with group 12 (syncLog). In the ideal case, we can import their code like the eventCreationTool above.~~
* ~~__Import logSync__~~
* ~~Interface testing: Can we import logSync?~~
* ~~Interface testing: Do callbacks work from python back to java?~~
* Remote add this repo to BACnet and pull.
* Implement packet splitting
* Implement advanced transport protocol
* Write specifications for integration for group 10 KotlinUI.
* Get logSync to run successfully
