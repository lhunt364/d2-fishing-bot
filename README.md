# d2-fishing-bot
A bot for Destiny 2 that will automatically fish for you.
## Instructions
The bot needs to know where on your screen the "Perfect Catch" prompt appears. Here is a step-by-step guide on how to tell it that:
1. Go to any fishing spot and start fishing as normal.
2. When you get the "Perfect Catch" prompt, instead of reeling your lure in, press Print Screen to take a screenshot.
3. Find this screenshot you just took. By default Destiny 2 saves screenshots at C:\Users\user\Documents\Destiny 2\Screenshots
4. Open this screenshot, and make it take up the same amount of space on your screen that your game takes up (E.G. if your game is fullscreen, make the screenshot fullscreen).
5. Open the fishing bot if you have not already. Make sure that the "Enable Binds" checkbox is checked, and the "Lock Coords" checkbox is unchecked.
6. Looking at the screenshot, find the "Perfect Catch" prompt. 
7. Place your mouse cursor at the top left corner of the "P" in "Perfect Catch" and press the coord1 auto set key bind (Default: Open Bracket).
8. Place your mouse cursor at the bottom right corner of the "h" in "Perfect Catch" and press the coord2 auto set key bind (Default: Close Bracket).
9. Check the "Lock Coords" checkbox.
10. Click the "Save Coords" button if you would like to save the coordinates you just gave the bot, so that you don't have to do this process again.

Now that the bot knows where to look for the "Perfect Catch" prompt, it is ready to be used.
To use the bot:
1. Go to any fishing spot and position yourself so that you have the "Go Fishing" prompt.
2. Press the "Start" key bind (Default: Minus) to start the bot.
3. When you want to stop fishing press the "Stop" key bind (Default: Equals) to stop the bot.

It's as simple as that. I highly recommend also having the "Enable Anti AFK" checkbox checked (it is by default), as this will periodically move your character around to stop you from getting kicked for being AFK.

## Configuration
The config.properties file can be used to configure the bot, including its key bindings. If you are using the Executable Jar File, it should be located in the same folder as said Jar file. If you are running the bot's source
code from an IDE or similar, you can find the file in the same folder as the src folder. Below is a list of all the properties in the config.properties file and what they all do, appropriate values for each of them, and their default values.

1. forward: The key bind for your forward movement. Should be a valid key. Default: w
2. start: The key bind for the "Start" button in the bot. Should be a valid key. Default: Minus
3. timeout: The time, in milliseconds, it takes for the bot to automatically stop after it has not caught any fish. Should be an integer. Default: 45000
4. afkinterval: The time, in milliseconds, between each instance of the bot moving your character around. Should be an integer. Default: 60000
5. fishbutton: The key bind for your fishing rod in game. It should be the same as your interact bind. Should be a valid key. Default: e
6. coordtwo: The key bind for setting the second coordinate for the bot. Should be a valid key. Default: Close Bracket
7. significance: The percentage that the white pixels to total pixels ratio of the area of your screen being tracked by the bot has to increase in order to trigger a reel. Should be a number between 0 and 1. Default: 0.1
8. white: The minimum red, green, and blue a pixel requires to be considered white. Should be an integer between 0 and 255. Default: 200
9. stop: The key bind for the "Stop" button in the bot. Should be a valid key. Default: Equals
10. coordone: The key bind for setting the first coordinate for the bot. Should be a valid key. Default: Open Bracket
11. y1: The y of the first coordinate. Should be an integer. Default: 0
12. backward: The key bind for your backward movement. Should be a valid key. Default: s
13. x1: The x of the first coordinate. Should be an integer. Default: 0
14. y2: The y of the second coordinate. Should be an integer. Default: 0
15. interval: The time, in milliseconds, between each check of the area of your screen that the bot is tracking. Should be an integer. Default: 50
16. x2: The x of the second coordinate. Should be an integer. Default: 0
17. jump: The key bind for your jump. Should be a valid key. Default: Space

## Key Bindings
In Version 1.2 and above, there is an "Update Bindings" button included in the bot, which allows to easily update key bindings without worrying about what a "valid key" is. Simply click the button, select what binding to change, press the key you want to rebind it to, and press confirm. 
Note that you need to restart the bot to apply the changes.

If for whatever reason you wanted to manually set these bindings, the below information explains how to do so. 
The bot uses jnativehook's NativeKeyEvent$getKeyText for key bindings. If you are wondering what a "valid key" is, as referred to in the above list, below is a list of most valid keys. 
For the sake of brevity, in this list, whenever a key is listed to be "itself", that just means that the key's String value is the same as the key name. (E.G. F1 is itself, so F1 is "F1". The space key is itself, so it is "Space"). 
Note that if you use these in the config.properties file, do not include quotation marks.
1. The Escape key is itself.
2. The function keys (F1, F2, ... F12) are themselves.
3. \` is "Back Quote".
4. The numeric keys (1, 2, ... 0) are themselves.
5. \- is "Minus".
6. = is "Equals".
7. The Backspace key is itself.
8. The Tab key is itself.
9. The alphabetical keys (a, b, ... z) are themselves.
10. \[ is "Open Bracket".
11. \] is "Close Bracket".
12. \ is "Back Slash".
13. ; is "Semicolon".
14. ' is "Quote".
15. The Enter key is itself.
16. , is "Comma".
17. . is "Period".
18. / is "Slash".
19. The space key is itself.
20. The edit keys and keys above them (Print Screen, ... Pause, Insert, ... Page Down) are all their unabbreviated selves.
21. The arrow keys are their directions (E.G. The up arrow key is "Up").
22. The Numeric Lock key is "Num Lock".
23. The modifier and control keys (Shift, Control, Alt) are themselves.
