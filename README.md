# qrCodeReaderWriter

## TODO:
* Write proper README
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
* __Encode and Decode qr code in base64 encoding__
* If Chaquopy works as exspected, add it to the documentation over on [BACnet](https://github.com/cn-uofbasel/BACnet/tree/master/groups/02-soundLink)
* Figure out how to use the [eventCreationTool](https://github.com/cn-uofbasel/BACnet/tree/master/groups/04-logMerge/eventCreationTool) on our android app, according to [this](https://chaquo.com/chaquopy/doc/current/java.html)
* Make a license file (preferably the same, BACnet uses)
* Get a [license for Chaquopy](https://chaquo.com/chaquopy/license/?app=ch.unibas.qrscanner)
* Specify Interface with group 12 (syncLog). In the ideal case, we can import their code like the eventCreationTool above.
