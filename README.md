Forge compatible World Downloader
=================================

Port of dslake's World Downloader (https://github.com/dslake/WorldDownloader). No vanilla class has been modified. Compatible with FTB unleashed.

This version is for people playing on 1.5.2 servers using Forge. It has been tested and works on FTB Unleashed. Any other 1.5.2 FTB should be compatible too.

THIS VERSION IS STILL IN BETA

Installation
------------
Like any other forge mod, place it in your mods folder and it's ready to go !

This mod is only client side, so you don't need to alter your server's jar

Usage
-----
In the 'ESC' menu, click on "Download this world". Every loaded chunk will be saved, and if you move, new chunk will be saved as well.

Downloading Storage
-------------------
IMPORTANT : if you want to download the content of your chests (or any other storage, for FTB users), you MUST open them to see their content. The server will send you the content only if you ask for it. 

Applied Energetics users : to download the content of your ME systems, open you ME drives block.


Upcoming Features
-----------------
 - Multiworld support
 - Better tile entities support : The current version can override a previous save keeping unloaded chunks in their previous state (ie they are not erased if you went there before but not this time). However, overwritten chunks are entirely rewritten, and 
 - 1.6+ support (maybe)