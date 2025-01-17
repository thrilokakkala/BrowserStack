package browserStack.test.tests;

import org.junit.jupiter.api.Test;

import browserStack.test.businessLibrary.BrowserStackExample;

public class BrowserStackExampleST extends BrowserStackExample {

    @Test
    public void browserStackExample() throws Exception {  
        browserStackRun();
    }
}
