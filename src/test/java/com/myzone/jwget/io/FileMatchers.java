package com.myzone.jwget.io;

import com.google.common.io.LineReader;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileMatchers {

    public static <T extends File> Matcher<? super T> contentEqualsToIgnoreEol(T expected) {
        return new ContentEqualsToMatcher<T>(expected) {
            @Override
            protected boolean isContentEqual(FileReader left, FileReader right) {
                LineReader leftLineReader = new LineReader(left);
                LineReader rightLineReader = new LineReader(right);

                try {
                   while (true) {
                       String leftLine = leftLineReader.readLine();
                       String rightLine = rightLineReader.readLine();

                       if (leftLine == null) {
                           return rightLine == null;
                       } else if (!leftLine.equals(rightLine)) {
                           return false;
                       }
                    }
                } catch (IOException e) {
                    return false;
                }
            }
        };
    }

    protected static abstract class ContentEqualsToMatcher<T extends File> extends TypeSafeDiagnosingMatcher<T> {

        protected final T expected;

        public ContentEqualsToMatcher(T expected) {
            this.expected = expected;
        }

        @Override
        protected boolean matchesSafely(T actual, Description description) {
            boolean actualExists = actual.exists();
            boolean expectedExists = expected.exists();

            if (actualExists && expectedExists) {
                try (FileReader actualStream = new FileReader(actual);
                     FileReader expectedStream = new FileReader(expected)) {
                    boolean equals = isContentEqual(actualStream, expectedStream);

                    if (!equals) {
                        description.appendText("content of ")
                                .appendValue(actual)
                                .appendText(" is not equal to content of ")
                                .appendValue(expected);
                    }

                    return equals;
                } catch (Exception e) {
                    return false;
                }
            } else if (actualExists != expectedExists) {
                description.appendValue(actual)
                        .appendText(String.format("%s exists,", actualExists ? "" : " doesn't"))
                        .appendText(", when ")
                        .appendValue(expected)
                        .appendText(expectedExists ? " exists" : " doesn't");

                return false;
            } else {
                // both of files aren't exist

                return true;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("content should be equal to content of ").appendValue(expected);
        }

        protected abstract boolean isContentEqual(FileReader l, FileReader r);

    }


}
