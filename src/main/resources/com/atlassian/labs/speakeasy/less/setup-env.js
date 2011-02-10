
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

    var ourImporter = function (path, paths, callback, env)
    {
        path = env.relpath + path;
        // TODO we should really resolve .. etc in path so that alreadySeen works

        var match = /^(.*\/)([^/]+)?$/.exec(path); // basename of path becomes new relpath
        var newrelpath = match ? match[1] : "";

        var data;
        if (env.alreadySeen[path]) {
            data = "/* skipping already included " + path + " */\n";
        } else {
            env.alreadySeen[path] = true;
            data = env.ourLoader.load(path);
            data = data + ""; // converts to proper native string
        }

        var newenv = {
            ourLoader: env.ourLoader,
            relpath: newrelpath,
            alreadySeen: env.alreadySeen,
            filename: path
        };


        var parser = new exports.Parser(newenv);
        parser.parse(data, function(e, root)
        {
            if (e)
            {
                java.lang.System.out.println("ERROR " + e); // TODO log better
            }
            callback(root);
        });
    };

    runLessRun = function(loader, css, compress) {
        if (typeof exports.Parser.importer == "undefined") {
            exports.Parser.importer = ourImporter;
        }

        var alreadySeen = {};
        var env = {
            ourLoader: loader,
            relpath: "", // blank or ends in a slash
            filename: "<rootfile>",
            alreadySeen: alreadySeen
        };
        var parser = new exports.Parser(env);

        var result;
        parser.parse(css, function (e, root) {
            if (e) {
                java.lang.System.out.println("ERROR " + e); // TODO log better
            }
            result = root.toCSS({compress: compress});
        });
        return result;
    }

})();

