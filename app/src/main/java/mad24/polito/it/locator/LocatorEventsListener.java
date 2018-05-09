package mad24.polito.it.locator;

public interface LocatorEventsListener
{
    void onSuccess(double latitude, double longitude);
    void onFailure();
    void onPermissionDenied();
}
