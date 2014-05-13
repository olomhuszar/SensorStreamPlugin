var sensorStreamPlugin =  {
    startEvent: function( serverAddress, token, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'SensorStreamPlugin',
            'startStream',
            [{
                "token": token,
                "serverAddress": serverAddress
            }]
        );
    },
    stopEvent: function(successCallback, errorCallback) {
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