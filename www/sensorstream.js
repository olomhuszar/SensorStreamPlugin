var sensorStreamPlugin =  {
    connect: function( ipAddress, port,  successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'SensorStreamPlugin',
            'connect',
            [{
                "ipAddress": ipAddress,
                "port": port
            }]
        );
    },
    disconnect: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'SensorStreamPlugin',
            'disconnect',
            []
        );
    },
    startStream: function( successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'SensorStreamPlugin',
            'startStream',
            []
        );
    },
    stopStream: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'SensorStreamPlugin',
            'stopStream',
            []
        );
    }
}
module.exports = sensorStreamPlugin;