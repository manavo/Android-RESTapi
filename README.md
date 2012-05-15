# Android-RESTapi

A simple Android RESTful library for integrating with RESTful JSON APIs

This library was developed as part of [Sprinter](http://sprinterapp.com). It has been tested and works fully with [Sprint.ly's](https://sprint.ly) [API](http://support.sprint.ly/kb/api).

## Installation

No .jar file to use at the moment, so you can copy the files into your project and reference them. Try and keep the directory structure as it is (com.manavo.rest), otherwise the packages might not quite match up.

## Basic Usage

It is recommended that you extend the RestApi class and add your methods there. That way you can keep all your actual endpoint references in that one file, and not worry about them in different parts of the code.

So, you can create a new class (I'm going to call it SprinterApi, seeing as that's what I called it in my app), and it should look something like this:

### Class init

```java
public class SprinterApi extends RestApi {
	
	public SprinterApi(Activity activity) {
		super(activity);
		
		this.BASE_URL = "https://sprint.ly/api/";
		this.urlSuffix = ".json";
		this.rest.setHost("sprint.ly");
		this.setUserAgent("sprinter");
		
		this.acceptAllSslCertificates();
       	this.authorize();
	}
}
```

This has now setup our class so it works with the endpoint format of Sprint.ly.

### Authorization

Not many APIs will work with no authentication whatsoever. Sprint.ly being no exception, it uses basic HTTP authentication to pair the email address and API token that each user has.

So, we create our authorize function, which looks like this:

```java
// return SprinterApi so we can chain calls
public SprinterApi authorize(String email, String token) {
	// email acts as the username and token as the password of the basic auth
	this.rest.authorize(email, token);
	return this;
}
```

### Making API calls

Now, for making specific calls and getting back our data:

```java
public void getProducts() {
	this.get("products");
}

public void getItems(Long id) {
	this.addParameter("limit", 100);
	this.get("products/"+id.toString()+"/items");
}
```

These are just some sample calls.

### But how do I get the return data?

On Android, it's good practice if you want to make HTTP calls, to make them on a background thread. Otherwise, the call will block the main UI thread and render the app unresponsive. Probably prompt the user about it as well, and offer them the option to kill the app. Which isn't the point of this exercise.

So, this library makes use of Android's AsyncTask class, to run the HTTP requests on a background thread, and once it's completed, it invokes a callback that you've specified.

For example:

```java
private void loadProducts() {
    SprinterApi api = new SprinterApi(this);
    
    api.setCallback(new RestCallback() {
		public void success(Object obj) {
			JSONArray data = (JSONArray)obj;
			
			setupView(data);
		}
	});
    api.getProducts();
}
```

In this simple example, once we receive the data, we call the setupView function, which initializes the view accordingly.

### Errors

Connection errors are handled automatically, and output [Toast notifications](http://developer.android.com/guide/topics/ui/notifiers/toasts.html) explaining what the problem was (the message shown is the description of the exception thrown).

Status code errors from the server however may differ from API to API. Some APIs, might return simply the error string. In this case, the library will just show a Toast notification again with that text.

If however, the API returns something else (it'll probably be JSON encoded data, won't it?), like Sprint.ly does, then you can set an error callback and handle it yourself.

Just like above we set a callback, we set our error callback as well, so we can handle the errors in greater detail:

```java
private void loadProducts() {
    SprinterApi api = new SprinterApi(this);
    
    api.setCallback(new RestCallback() {
		public void success(Object obj) {
			JSONArray data = (JSONArray)obj;
			
			setupView(data);
		}
	});
	api.setErrorCallback(new RestErrorCallback() {
		@Override
		public void error(String data) {
			String message;
			int code;
			// try to read the JSON Object. If it fails, just show the data.
			try {
				JSONObject obj = new JSONObject(data);
				message = obj.getString("message");
				code = obj.getInt("code");
			} catch (JSONException e) {
				e.printStackTrace();
				message = data;
				code = -1;
			}
			
			Toast.makeText(ViewProducts.this, message, Toast.LENGTH_LONG).show();
		}
	});
    api.getProducts();
}
```

So here, we've manually stripped out the message form the JSON object returned, and show it to the user. You could obviously do much fancier things here, but we're just demonstrating in this case.

If you want to handle all status code errors without needing to write this for each API call, just override the onStatusCodeError method in your class extending RestApi.\

For example:

```java
public void onStatusCodeError(String data) {
	if (this.errorCallback != null) {
		this.errorCallback.error(data);
	} else {
		String message;
		int code;
		try {
			JSONObject obj = new JSONObject(data);
			message = obj.getString("message");
			code = obj.getInt("code");
		} catch (JSONException e) {
			e.printStackTrace();
			message = data;
			code = -1;
		}
		
		if (code == 403) {
			Toast.makeText(this.activity, "We could not authenticate you on Sprintly. Please try again.", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this.activity, message, Toast.LENGTH_LONG).show();
		}
	}
}
```

Here, we manually handle the case of getting a 403, and show a different message than the default one that Sprint.ly returns.

## Advanced Usage

### Caching

I need to write something for this as well, but I've written enough for one night!