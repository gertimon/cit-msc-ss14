'use strict';
var LIVERELOAD_PORT = 35729;
var SERVER_PORT = 9000;
var lrSnippet = require('connect-livereload')({port: LIVERELOAD_PORT});
var mountFolder = function (connect, dir) {
    return connect.static(require('path').resolve(dir));
};
var rewriteRulesSnippet = function myMiddleware(req, res, next) {
    req.url = req.url.replace(new RegExp('^/app/javascripts/(.*\\.coffee)$'), '/javascripts/$1');
    next();
};

// # Globbing
// for performance reasons we're only matching one level down:
// 'test/spec/{,*/}*.js'
// use this if you want to match all subfolders:
// 'test/spec/**/*.js'
// templateFramework: 'lodash'

module.exports = function (grunt) {
    // show elapsed time at the end
    require('time-grunt')(grunt);
    // load all grunt tasks
    require('load-grunt-tasks')(grunt);

    grunt.initConfig({
        watch: {
            options: {
                nospawn: true,
                livereload: true
            },
            configFiles: {
                files: [ 'Gruntfile.js' ],
                options: { reload: true }
            },
            livereload: {
                options: {
                    livereload: grunt.option('livereloadport') || LIVERELOAD_PORT
                },
                files: [
                    'app/*.html',
                    '{.tmp,app}/stylesheets/{,*/}*.{css,less}',
                    '{.tmp,app}/javascripts/{,*/}*.{js,coffee}',
                    'app/images/{,*/}*.{png,jpg,jpeg,gif,webp}',
                    'app/javascripts/templates/*.{ejs,mustache,hbs,hamlc}',
                    'test/spec/**/*.js'
                ]
            },
            haml: {
                files: [ 'app/*.haml' ],
                tasks: [ 'haml' ],
            },
            less: {
                files: [ 'app/stylesheets/*.less' ],
                tasks: ['less:development']
            },
            coffee: {
                files: [ 'app/javascripts/*.coffee' ],
                tasks: ['coffee:development']
            }
        },
        connect: {
            options: {
                port: grunt.option('port') || SERVER_PORT,
                // debug: true,
                // change this to '0.0.0.0' to access the server from outside
                hostname: 'localhost'
            },
            livereload: {
                options: {
                    middleware: function (connect) {
                        return [
                            lrSnippet,
                            rewriteRulesSnippet,
                            mountFolder(connect, '.tmp'),
                            mountFolder(connect, 'app')
                        ];
                    }
                }
            },
            dist: {
                options: {
                    middleware: function (connect) {
                        return [
                            mountFolder(connect, 'dist')
                        ];
                    }
                }
            }
        },
        clean: {
            dist: ['.tmp', 'dist/*'],
            server: '.tmp'
        },
        less: {
            development: {
                options: {
                    paths: [ 'app/stylesheets', 'app/bower_components/bootstrap/less' ],
                    modifyVars: { 'icon-font-path': '"../bower_components/bootstrap/fonts/"' }
                },
                files: {
                    '.tmp/stylesheets/main.css': 'app/stylesheets/main.less'
                }
            },
            production: {
                options: {
                    paths: [ 'app/stylesheets', 'app/bower_components/bootstrap/less' ],
                    cleancss: true,
                    modifyVars: { 'icon-font-path': '"../fonts/"' }
                },
                files: {
                    'dist/stylesheets/main.css': 'app/stylesheets/main.less'
                }
            }
        },
        haml: {
            development: {
                files: {
                    '.tmp/index.html': 'app/index.haml'
                }
            }
        },
        coffee: {
            development: {
                expand: true,
                cwd: 'app/javascripts',
                src: [ '*.coffee' ],
                dest: '.tmp/javascripts',
                ext: '.js',
                options: { sourceMap: true }
            }
        },
        // not enabled since usemin task does concat and uglify
        // check index.html to edit your build targets
        // enable this task if you prefer defining your build targets here
        /*uglify: {
            dist: {}
        },*/
        useminPrepare: {
            html: 'app/index.haml',
            options: {
                dest: 'dist'
            }
        },
        usemin: {
            html: ['dist/{,*/}*.html'],
            css: ['dist/stylesheets/{,*/}*.css'],
            options: {
                dirs: ['dist']
            }
        },
        imagemin: {
            dist: {
                files: [{
                    expand: true,
                    cwd: 'app/images',
                    src: '{,*/}*.{png,jpg,jpeg}',
                    dest: 'dist/images'
                }]
            }
        },
        htmlmin: {
            dist: {
                options: {
                    /*removeCommentsFromCDATA: true,
                    // https://github.com/yeoman/grunt-usemin/issues/44
                    collapseBooleanAttributes: true,
                    removeAttributeQuotes: true,
                    removeRedundantAttributes: true,
                    useShortDoctype: true,
                    removeEmptyAttributes: true,
                    removeOptionalTags: true
                    removeComments: false,
                    collapseWhitespace: true */ 
                },
                files: [{
                    expand: true,
                    cwd: '.tmp',
                    src: '*.html',
                    dest: 'dist'
                }]
            },
            deploy: {
                options: {
                    /*removeCommentsFromCDATA: true,
                    // https://github.com/yeoman/grunt-usemin/issues/44
                    collapseBooleanAttributes: true,
                    removeAttributeQuotes: true,
                    removeRedundantAttributes: true,
                    useShortDoctype: true,
                    removeEmptyAttributes: true,
                    removeOptionalTags: true */
                    removeComments: true,
                    collapseWhitespace: true
                },
                files: [{
                    expand: true,
                    cwd: 'dist',
                    src: '*.html',
                    dest: 'dist'
                }]
            }
        },
        copy: {
            dist: {
                files: [{
                    expand: true,
                    dot: true,
                    cwd: 'app',
                    dest: 'dist',
                    src: [ 'images/{,*/}*.{webp,gif}' ]
                }, {
                    expand: true,
                    cwd: 'app',
                    dest: 'dist/fonts',
                    flatten: true,
                    src: [ 'bower_components/bootstrap/fonts/*.*' ]
                }]
            }
        },
        filerev: {
            options: {
                encoding: 'utf8',
                length: 16
            },
            dist: {
                src: [
                    'dist/javascripts/{,*/}*.js',
                    'dist/stylesheets/{,*/}*.css',
                    'dist/images/{,*/}*.{png,jpg,jpeg,gif,webp}',
                    'dist/fonts/*.*'
                ]
            }
        }
    });

    grunt.registerTask('serve', function (target) {
        if (target === 'dist') {
            return grunt.task.run(['build', 'connect:dist:keepalive']);
        }

        grunt.task.run([
            'clean:server',
            'less:development',
            'haml:development',
            'coffee:development',
            'connect:livereload',
            'watch'
        ]);
    });

    grunt.registerTask('build', [
        'clean:dist',
        'haml',
        'coffee',
        'useminPrepare',
        'imagemin',
        'htmlmin:dist',
        'concat',
        'less:production',
        'uglify',
        'copy',
        'filerev',
        'usemin',
        'htmlmin:deploy'
    ]);

    grunt.registerTask('default', [
        'build'
    ]);
};
