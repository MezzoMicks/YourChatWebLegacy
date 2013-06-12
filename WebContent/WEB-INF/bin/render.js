var page = require('webpage').create(), system = require('system');

if (system.args.length < 3) {
    console.log('Usage: render.js <some URL> <Targetfile>');
    phantom.exit();
}

page.viewportSize = { width: 800, height: 600 };
page.clipRect = { top: 0, left: 0, width: 800, height: 600 };
page.open(system.args[1], function () {
	page.render(system.args[2]);
	console.log("done");
    phantom.exit();
});