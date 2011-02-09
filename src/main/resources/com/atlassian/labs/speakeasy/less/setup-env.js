
var runLessRun;

var exports = {}; // exports for parser.js
var require;
(function() {
    var tree = {};
    require = function(arg) {
        if (arg == "less/tree") {
            return tree;
        }
        throw "Attempt to require module other than 'less/tree': " + arg;
    };

    var ourImporter = function (path, paths, callback, env) {
        java.lang.System.out.println("QQQQ " + env.relpath + " " + path );
        path = env.relpath + path;

        var match = /^(.*\/)([^/]+)?$/.exec(path); // basename of path becomes new relpath
        var newrelpath = match ? match[1] : "";
        java.lang.System.out.println("RRRR " + path + " " + newrelpath);
        var data = env.ourLoader.load(path) + ""; // converts to native string
        var newenv = {
            ourLoader: env.ourLoader,
            relpath: newrelpath
        };
        var parser = new exports.Parser(newenv);
        parser.parse(data, function(e, root) {
            if (e) {
                java.lang.System.out.println("ERROR " + e);
            }
            callback(root);
        });
    };

    runLessRun = function(loader, css, compress) {
        if (typeof exports.Parser.importer == "undefined") {
            exports.Parser.importer = ourImporter;
        }

        var env = {
            ourLoader: loader,
            relpath: "" // blank or ends in a slash
        };
        var parser = new exports.Parser(env);

        var result;
        parser.parse(css, function (e, root) {
            if (e) {
                java.lang.System.out.println("ERROR " + e);
            }
            result = root.toCSS({compress: compress});
        });
        return result;
    }

})();

