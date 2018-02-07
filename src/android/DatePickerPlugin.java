package com.plugin.datepicker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.TimePicker;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

@SuppressLint("NewApi")
public class DatePickerPlugin extends CordovaPlugin {

    private static final String ACTION_DATE = "date";
    private static final String ACTION_TIME = "time";
    private static final String RESULT_ERROR = "error";
    private static final String RESULT_CANCEL = "cancel";

    // On some devices, onDateSet or onTimeSet are being called twice
    private boolean called = false;
    private boolean canceled = false;

    private DatePickerDialog dateDialog;
    private TimePickerDialog timeDialog;

    @Override
    public boolean execute(final String action, final JSONArray data, final CallbackContext callbackContext) {
        called = false;
        canceled = false;
        if ("show".equals(action)) {
            show(data, callbackContext);
        } else if ("close".equals(action)) {
            close();
        } else {
            callbackContext.error("Not supported action");
            return false;
        }
        return true;
    }

    public void close() {
        if (this.dateDialog != null && this.dateDialog.isShowing()) {
            this.dateDialog.dismiss();
        } else if (this.timeDialog != null && this.timeDialog.isShowing()) {
            this.timeDialog.dismiss();
        }
    }

    public synchronized void show(final JSONArray data, final CallbackContext callbackContext) {
        DatePickerPlugin datePickerPlugin = this;
        Context currentCtx = cordova.getActivity();
        Runnable runnable;
        JsonDate jsonDate = new JsonDate().fromJson(data);
        if (!jsonDate.locale.isEmpty()) {
            String[] localeParams = jsonDate.locale.split("_");
            if (localeParams.length > 1) {
                currentCtx.getResources().getConfiguration().setLocale(new Locale(localeParams[0], localeParams[1]));
            } else {
                currentCtx.getResources().getConfiguration().setLocale(new Locale(localeParams[0]));
            }
        }

        JSONObject options = data.optJSONObject(0);
        int theme = options.optString("androidTheme").isEmpty()
                ? 1
                : getStyle(options.optString("androidTheme"));

        if (ACTION_TIME.equalsIgnoreCase(jsonDate.action)) {
            runnable = runnableTimeDialog(datePickerPlugin, theme, currentCtx,
                    callbackContext, jsonDate, Calendar.getInstance(TimeZone.getDefault()));
        } else {
            runnable = runnableDatePicker(datePickerPlugin, theme, currentCtx, callbackContext, jsonDate);
        }
        cordova.getActivity().runOnUiThread(runnable);
    }

    private TimePicker timePicker;
    private int timePickerHour = 0;
    private int timePickerMinute = 0;

    private Runnable runnableTimeDialog(final DatePickerPlugin datePickerPlugin,
                                        final int theme,
                                        final Context currentCtx,
                                        final CallbackContext callbackContext,
                                        final JsonDate jsonDate,
                                        final Calendar calendarDate) {
        return new Runnable() {
            @Override
            public void run() {
                final TimeSetListener timeSetListener = new TimeSetListener(datePickerPlugin, callbackContext, calendarDate);
                timeDialog = new TimePickerDialog(
                        currentCtx,
                        theme,
                        timeSetListener,
                        jsonDate.hour,
                        jsonDate.minutes,
                        jsonDate.is24Hour) {
                    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                        timePicker = view;
                        timePickerHour = hourOfDay;
                        timePickerMinute = minute;
                    }
                };
                timeDialog.setCancelable(true);
                timeDialog.setCanceledOnTouchOutside(false);

                if (!jsonDate.titleText.isEmpty()) {
                    timeDialog.setTitle(jsonDate.titleText);
                }
                if (!jsonDate.nowText.isEmpty()) {
                    timeDialog.setButton(DialogInterface.BUTTON_NEUTRAL, jsonDate.nowText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (timePicker != null) {
                                Calendar now = Calendar.getInstance();
                                timeSetListener.onTimeSet(timePicker, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
                            }
                        }
                    });
                }
                String labelCancel = jsonDate.cancelText.isEmpty() ? currentCtx.getString(android.R.string.cancel) : jsonDate.cancelText;
                timeDialog.setButton(DialogInterface.BUTTON_NEGATIVE, labelCancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        canceled = true;
                        callbackContext.error(RESULT_CANCEL);
                    }
                });
                String labelOk = jsonDate.okText.isEmpty() ? currentCtx.getString(android.R.string.ok) : jsonDate.okText;
                timeDialog.setButton(DialogInterface.BUTTON_POSITIVE, labelOk, timeDialog);
                timeDialog.show();
                timeDialog.updateTime(new Random().nextInt(23), new Random().nextInt(59));
                timeDialog.updateTime(jsonDate.hour, jsonDate.minutes);
            }
        };
    }

    private Runnable runnableDatePicker(final DatePickerPlugin datePickerPlugin,
                                        final int theme,
                                        final Context currentCtx,
                                        final CallbackContext callbackContext,
                                        final JsonDate jsonDate) {
        return new Runnable() {
            @Override
            public void run() {
                final DateSetListener dateSetListener = new DateSetListener(
                        datePickerPlugin,
                        theme,
                        callbackContext,
                        jsonDate
                );
                dateDialog = new DatePickerDialog(
                        currentCtx,
                        theme,
                        dateSetListener,
                        jsonDate.year,
                        jsonDate.month,
                        jsonDate.day
                );
                prepareDialog(dateDialog, dateSetListener, callbackContext, currentCtx, jsonDate);
                dateDialog.show();
            }
        };
    }

    private void prepareDialog(final DatePickerDialog dateDialog, final OnDateSetListener dateListener,
                               final CallbackContext callbackContext, Context currentCtx, JsonDate jsonDate) {
        dateDialog.setCancelable(true);
        dateDialog.setCanceledOnTouchOutside(false);
        if (!jsonDate.titleText.isEmpty()) {
            dateDialog.setTitle(jsonDate.titleText);
        }
        if (!jsonDate.todayText.isEmpty()) {
            dateDialog.setButton(DialogInterface.BUTTON_NEUTRAL, jsonDate.todayText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Calendar now = Calendar.getInstance();
                    DatePicker datePicker = dateDialog.getDatePicker();
                    dateListener.onDateSet(datePicker, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
                }
            });
        }
        String labelCancel = jsonDate.cancelText.isEmpty() ? currentCtx.getString(android.R.string.cancel) : jsonDate.cancelText;
        dateDialog.setButton(DialogInterface.BUTTON_NEGATIVE, labelCancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                canceled = true;
                callbackContext.error(RESULT_CANCEL);
            }
        });
        String labelOk = jsonDate.okText.isEmpty() ? currentCtx.getString(android.R.string.ok) : jsonDate.okText;
        dateDialog.setButton(DialogInterface.BUTTON_POSITIVE, labelOk, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DatePicker datePicker = dateDialog.getDatePicker();
                datePicker.clearFocus();
                dateListener.onDateSet(datePicker, datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
            }
        });

        DatePicker dp = dateDialog.getDatePicker();
        if (jsonDate.minDate > 0) {
            dp.setMinDate(jsonDate.minDate);
        }
        if (jsonDate.maxDate > 0 && jsonDate.maxDate > jsonDate.minDate) {
            dp.setMaxDate(jsonDate.maxDate);
        }
    }

    private int getStyle(String id) {
        return getResource(id, "style");
    }

    private int getResource(String id, String type) {
        return cordova.getActivity().getResources().getIdentifier(id, type, cordova.getActivity().getPackageName());
    }

    private final class DateSetListener implements OnDateSetListener {
        private JsonDate jsonDate;
        private final DatePickerPlugin datePickerPlugin;
        private final CallbackContext callbackContext;
        private final int theme;

        private DateSetListener(DatePickerPlugin datePickerPlugin, int theme, CallbackContext callbackContext, JsonDate jsonDate) {
            this.datePickerPlugin = datePickerPlugin;
            this.callbackContext = callbackContext;
            this.jsonDate = jsonDate;
            this.theme = theme;
        }

        /**
         * Return a string containing the date in the format YYYY/MM/DD or call TimeDialog if action != date
         */
        @Override
        public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
            if (canceled || called) {
                return;
            }
            called = true;
            canceled = false;
            if (ACTION_DATE.equalsIgnoreCase(jsonDate.action)) {
                String returnDate = year + "/" + (monthOfYear + 1) + "/" + dayOfMonth;
                callbackContext.success(returnDate);
            } else {
                // Open time dialog
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, monthOfYear);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                cordova.getActivity().runOnUiThread(runnableTimeDialog(
                        datePickerPlugin,
                        theme,
                        cordova.getActivity(),
                        callbackContext,
                        jsonDate,
                        selectedDate)
                );
            }
        }
    }

    private final class TimeSetListener implements OnTimeSetListener {
        private Calendar calendarDate;
        private final CallbackContext callbackContext;

        private TimeSetListener(DatePickerPlugin datePickerPlugin, CallbackContext callbackContext, Calendar selectedDate) {
            this.callbackContext = callbackContext;
            this.calendarDate = selectedDate != null ? selectedDate : Calendar.getInstance();
        }

        /**
         * Return the current date with the time modified as it was set in the
         * time picker.
         */
        @Override
        public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
            if (canceled) {
                return;
            }
            calendarDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendarDate.set(Calendar.MINUTE, minute);
            calendarDate.set(Calendar.SECOND, 0);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String toReturn = sdf.format(calendarDate.getTime());

            callbackContext.success(toReturn);
        }
    }

    private final class JsonDate {
        private String action = ACTION_DATE;
        private String titleText = "";
        private String okText = "";
        private String cancelText = "";
        private String todayText = "";
        private String nowText = "";
        private String locale = "";
        private long minDate = 0;
        private long maxDate = 0;
        private int month = 0;
        private int day = 0;
        private int year = 0;
        private int hour = 0;
        private int minutes = 0;
        private boolean is24Hour = false;

        JsonDate() {
            reset(Calendar.getInstance());
        }

        private void reset(Calendar c) {
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
            hour = c.get(Calendar.HOUR_OF_DAY);
            minutes = c.get(Calendar.MINUTE);
        }

        JsonDate fromJson(JSONArray data) {
            try {
                JSONObject obj = data.getJSONObject(0);
                action = isNotEmpty(obj, "mode") ? obj.getString("mode") : ACTION_DATE;

                minDate = isNotEmpty(obj, "minDate") ? obj.getLong("minDate") : 0L;
                maxDate = isNotEmpty(obj, "maxDate") ? obj.getLong("maxDate") : 0L;

                titleText = isNotEmpty(obj, "titleText") ? obj.getString("titleText") : "";
                okText = isNotEmpty(obj, "okText") ? obj.getString("okText") : "";
                cancelText = isNotEmpty(obj, "cancelText") ? obj.getString("cancelText") : "";
                todayText = isNotEmpty(obj, "todayText") ? obj.getString("todayText") : "";
                nowText = isNotEmpty(obj, "nowText") ? obj.getString("nowText") : "";
                is24Hour = isNotEmpty(obj, "is24Hour") && obj.getBoolean("is24Hour");
                locale = isNotEmpty(obj, "locale") ? obj.getString("locale") : "";

                String optionDate = obj.getString("date");

                String[] datePart = optionDate.split("/");
                month = Integer.parseInt(datePart[0]) - 1;
                day = Integer.parseInt(datePart[1]);
                year = Integer.parseInt(datePart[2]);
                hour = Integer.parseInt(datePart[3]);
                minutes = Integer.parseInt(datePart[4]);
            } catch (JSONException e) {
                reset(Calendar.getInstance());
            }
            return this;
        }

        boolean isNotEmpty(JSONObject object, String key) throws JSONException {
            return object.has(key)
                    && !object.isNull(key)
                    && object.get(key).toString().length() > 0
                    && !JSONObject.NULL.toString().equals(
                    object.get(key).toString());
        }
    }
}
