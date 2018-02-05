var exec = require("cordova/exec");

function DatePicker() {
  this.name = "DatePickerPlugin";
}

DatePicker.prototype.show = function (options, cb) {
  var padDate = function (date) {
    if (date.length === 1) {
      return ("0" + date);
    }
    return date;
  };

  var formatDate = function (date) {
    // date/minDate/maxDate will be string at second time
    if (!(date instanceof Date)) {
      date = new Date(date)
    }
    date = date.getFullYear()
      + "-"
      + padDate(date.getMonth() + 1)
      + "-"
      + padDate(date.getDate())
      + "T"
      + padDate(date.getHours())
      + ":"
      + padDate(date.getMinutes())
      + ":00Z";

    return date
  };

  if (options.date) {
    options.date = formatDate(options.date);
  }

  if (options.minDate) {
    options.minDate = formatDate(options.minDate);
  }

  if (options.maxDate) {
    options.maxDate = formatDate(options.maxDate);
  }

  if (options.popoverArrowDirection) {
    options.popoverArrowDirection = this._popoverArrowDirectionIntegerFromString(options.popoverArrowDirection);
    console.log("ha options", this, options.popoverArrowDirection);
  }

  var params = {
    mode: "date",
    date: new Date(),
    allowOldDates: true,
    allowFutureDates: true,
    minDate: "",
    maxDate: "",
    doneButtonLabel: "Ок",
    doneButtonColor: "#000",
    cancelButtonLabel: "Отмена",
    cancelButtonColor: "#000",
    x: "0",
    y: "0",
    minuteInterval: 1,
    popoverArrowDirection: this._popoverArrowDirectionIntegerFromString("any"),
    locale: "ru_RU"
  };

  for (var key in params) {
    if (typeof options[key] !== "undefined")
      params[key] = options[key];
  }
  this._callback = cb;

  exec(null, null, "DatePicker", "show", [params]);
};

DatePicker.prototype._dateSelected = function (date) {
  var d = new Date(parseFloat(date) * 1000);
  if (this._callback)
    this._callback(d);
};

DatePicker.prototype._dateSelectionCanceled = function () {
  if (this._callback)
    this._callback();
};

DatePicker.prototype._UIPopoverArrowDirection = {
  "up": 1,
  "down": 2,
  "left": 4,
  "right": 8,
  "any": 15
};

DatePicker.prototype._popoverArrowDirectionIntegerFromString = function (string) {
  if (typeof this._UIPopoverArrowDirection[string] !== "undefined") {
    return this._UIPopoverArrowDirection[string];
  }
  return this._UIPopoverArrowDirection.any;
};

module.exports = new DatePicker();
