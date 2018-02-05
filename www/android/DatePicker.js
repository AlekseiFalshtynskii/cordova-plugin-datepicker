var exec = require('cordova/exec');

function DatePicker() {
  this.name = 'DatePickerPlugin';
}

DatePicker.prototype.show = function(options, successCallback, errorCallback) {

  if (options.date && options.date instanceof Date) {
    options.date = (options.date.getMonth() + 1) + '/'
      + options.date.getDate() + '/'
      + options.date.getFullYear() + '/'
      + options.date.getHours() + '/'
      + options.date.getMinutes();
  }

  var params = {
    mode: 'date',
    date: '',
    minDate: 0,
    maxDate: 0,
    titleText: '',
    cancelText: '',
    okText: '',
    todayText: '',
    nowText: '',
    is24Hour: false,
    locale: 'ru_RU',
    androidTheme: 'DatePickerDefaultTheme'
  };

  for (var key in params) {
    if (typeof options[key] !== 'undefined') {
      if ((key === 'minDate' || key === 'maxDate') && options[key] instanceof Date) {
        params[key] = options[key].getTime();
      } else {
        params[key] = options[key];
      }
    }
  }

  var callback = function(message) {
    if (message !== 'error') {
      var timestamp = Date.parse(message);
      if (isNaN(timestamp) === false) {
        successCallback(new Date(message));
      } else {
        successCallback();
      }
    } else {
      // TODO error popup?
    }
  };

  var errCallback = function(message) {
    if (typeof errorCallback === 'function') {
      errorCallback(message);
    }
  };

  exec(callback, errCallback, 'DatePickerPlugin', 'show', [params]);
};

DatePicker.prototype.close = function(successCallback, errorCallback) {
  exec(successCallback, errorCallback, 'DatePickerPlugin', 'close');
};

module.exports = new DatePicker();
