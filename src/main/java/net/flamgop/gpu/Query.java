package net.flamgop.gpu;

import static org.lwjgl.opengl.GL46.*;

public class Query {

    public record QueryCloser(Query query) implements AutoCloseable {
        @Override
        public void close() {
            query.end();
        }
    }

    public enum QueryTarget {
        SAMPLES_PASSED(GL_SAMPLES_PASSED),
        TIMESTAMP(GL_TIMESTAMP),
        PRIMITIVES_GENERATED(GL_PRIMITIVES_GENERATED),
        ANY_SAMPLES_PASSED(GL_ANY_SAMPLES_PASSED),
        TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN(GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN),
        ANY_SAMPLES_PASSED_CONSERVATIVE(GL_ANY_SAMPLES_PASSED_CONSERVATIVE),
        TIME_ELAPSED(GL_TIME_ELAPSED),
        ;

        final int glQualifier;
        QueryTarget(int glQualifier) {
            this.glQualifier = glQualifier;
        }
    }

    private final int handle;
    private final QueryTarget target;

    public Query(QueryTarget target) {
        this.handle = glCreateQueries(target.glQualifier);
        this.target = target;
    }

    public int handle() {
        return handle;
    }

    public void label(String label) {
        glObjectLabel(GL_QUERY, handle, label);
    }

    public QueryCloser begin() {
        glBeginQuery(target.glQualifier, handle);
        return new QueryCloser(this);
    }

    public void end() {
        glEndQuery(target.glQualifier);
    }

    public boolean isResultAvailable() {
        return glGetQueryObjecti(handle, GL_QUERY_RESULT_AVAILABLE) == GL_TRUE;
    }

    public int getResult() {
        return glGetQueryObjecti(handle, GL_QUERY_RESULT);
    }

    public long getResult64() {
        return glGetQueryObjecti64(handle, GL_QUERY_RESULT);
    }

    public void delete() {
        glDeleteQueries(handle);
    }
}
