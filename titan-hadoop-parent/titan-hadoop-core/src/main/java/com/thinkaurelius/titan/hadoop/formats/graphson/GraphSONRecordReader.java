package com.thinkaurelius.titan.hadoop.formats.graphson;

import static com.thinkaurelius.titan.hadoop.compat.HadoopCompatLoader.DEFAULT_COMPAT;

import com.thinkaurelius.titan.hadoop.FaunusVertex;
import com.thinkaurelius.titan.hadoop.formats.VertexQueryFilter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.LineRecordReader;

import java.io.IOException;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GraphSONRecordReader extends RecordReader<NullWritable, FaunusVertex> {

    private Configuration configuration;
    private final LineRecordReader lineRecordReader;
    private final VertexQueryFilter vertexQuery;
    private FaunusVertex vertex = null;

    public GraphSONRecordReader(VertexQueryFilter vertexQuery) {
        this.lineRecordReader = new LineRecordReader();
        this.vertexQuery = vertexQuery;
    }

    @Override
    public void initialize(final InputSplit genericSplit, final TaskAttemptContext context) throws IOException {
        this.lineRecordReader.initialize(genericSplit, context);
        this.configuration = DEFAULT_COMPAT.getContextConfiguration(context);
    }

    @Override
    public boolean nextKeyValue() throws IOException {
        if (!this.lineRecordReader.nextKeyValue())
            return false;

        this.vertex = HadoopGraphSONUtility.fromJSON(this.configuration, this.lineRecordReader.getCurrentValue().toString());
        this.vertex.setConf(this.configuration);
        this.vertexQuery.defaultFilter(this.vertex);
        return true;
    }

    @Override
    public NullWritable getCurrentKey() {
        return NullWritable.get();
    }

    @Override
    public FaunusVertex getCurrentValue() {
        return this.vertex;
    }

    @Override
    public float getProgress() throws IOException {
        return this.lineRecordReader.getProgress();
    }

    @Override
    public synchronized void close() throws IOException {
        this.lineRecordReader.close();
    }
}