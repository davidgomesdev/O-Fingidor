const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');

config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                from: path.resolve(__dirname, '../../../../composeApp/build/processedResources/js/main'),
                to: '.',
                noErrorOnMissing: false,
            }
        ]
    })
);

