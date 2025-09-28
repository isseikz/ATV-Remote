// Suppress webpack critical dependency warnings
if (typeof config !== 'undefined') {
    config.ignoreWarnings = [
        {
            module: /node_modules/,
            message: /Critical dependency/
        },
        /Critical dependency: the request of a dependency is an expression/
    ];

    // Ensure proper MIME types
    config.devServer = config.devServer || {};
    config.devServer.static = config.devServer.static || {};
    config.devServer.static.directory = config.devServer.static.directory || 'build/dist/wasmJs/developmentExecutable';
}

// Alternative approach: filter out critical dependency warnings
const originalWarn = console.warn;
console.warn = function(message) {
    if (typeof message === 'string' && message.includes('Critical dependency')) {
        return;
    }
    originalWarn.apply(console, arguments);
};