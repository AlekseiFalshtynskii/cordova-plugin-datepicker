#!/usr/bin/env node
var fs = require('fs');
var path = require('path');
var et = require('elementtree');

module.exports = function(ctx) {
  var etree = et.parse(fs.readFileSync(path.join(ctx.opts.projectRoot, 'config.xml')).toString());
  var preferences = etree.findall('*/preference/[@name="DatePickerColor"]');
  if (preferences.length) {
    var color = preferences[0].get('value');
    var xmlPath = path.join(ctx.opts.projectRoot, 'plugins/cordova-plugin-datepicker/res/android/values/date-picker.xml');
    etree = et.parse(fs.readFileSync(xmlPath).toString());
    var items = etree.findall('*/item/[@name="colorAccent"]');
    if (items.length) {
      items[0].text = color;
      fs.writeFileSync(xmlPath, etree.write(), {encoding: "UTF8"});
    }
  }
};
