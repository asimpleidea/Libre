package mad24.polito.it.locator;

public interface LocationNotifier
{
    Locator addListener(LocatorEventsListener locationListener);
}
