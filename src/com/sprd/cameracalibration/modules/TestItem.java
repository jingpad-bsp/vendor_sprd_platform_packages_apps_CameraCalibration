package com.sprd.cameracalibration.modules;

public class TestItem {

    private String testName;
    private String testPackageName;
    private String testClassName;
    private int testResult;

    public TestItem(String testName_, String testPackageName_,
            String testClassName_, int testResult_) {
        testName = testName_;
        testPackageName = testPackageName_;
        testClassName = testClassName_;
        testResult = testResult_;
    }

    public String getTestName() {
        return testName;
    }

    public String getTestPackageName() {
        return testPackageName;
    }

    public String getTestClassName() {
        return testClassName;
    }

    public int getTestResult() {
        return testResult;
    }

    public void setTestResult(int result) {
        testResult = result;
    }
}
