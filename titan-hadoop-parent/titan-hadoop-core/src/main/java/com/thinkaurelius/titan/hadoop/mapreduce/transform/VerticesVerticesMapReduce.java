package com.thinkaurelius.titan.hadoop.mapreduce.transform;

import static com.thinkaurelius.titan.hadoop.compat.HadoopCompatLoader.DEFAULT_COMPAT;

import com.thinkaurelius.titan.hadoop.FaunusVertex;
import com.thinkaurelius.titan.hadoop.StandardFaunusEdge;
import com.thinkaurelius.titan.hadoop.Holder;
import com.thinkaurelius.titan.hadoop.Tokens;
import com.thinkaurelius.titan.hadoop.mapreduce.util.EmptyConfiguration;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

import static com.tinkerpop.blueprints.Direction.*;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class VerticesVerticesMapReduce {

    public static final String DIRECTION = Tokens.makeNamespace(VerticesVerticesMapReduce.class) + ".direction";
    public static final String LABELS = Tokens.makeNamespace(VerticesVerticesMapReduce.class) + ".labels";

    public enum Counters {
        EDGES_TRAVERSED
    }

    public static Configuration createConfiguration(final Direction direction, final String... labels) {
        final Configuration configuration = new EmptyConfiguration();
        configuration.set(DIRECTION, direction.name());
        configuration.setStrings(LABELS, labels);
        return configuration;
    }

    public static class Map extends Mapper<NullWritable, FaunusVertex, LongWritable, Holder> {

        private Direction direction;
        private String[] labels;

        private final Holder<FaunusVertex> holder = new Holder<FaunusVertex>();
        private final LongWritable longWritable = new LongWritable();


        @Override
        public void setup(final Mapper.Context context) throws IOException, InterruptedException {
            this.direction = Direction.valueOf(context.getConfiguration().get(DIRECTION));
            this.labels = context.getConfiguration().getStrings(LABELS, new String[0]);
        }

        @Override
        public void map(final NullWritable key, final FaunusVertex value, final Mapper<NullWritable, FaunusVertex, LongWritable, Holder>.Context context) throws IOException, InterruptedException {

            if (value.hasPaths()) {
                long edgesTraversed = 0l;
                if (this.direction.equals(OUT) || this.direction.equals(BOTH)) {
                    for (final Edge edge : value.getEdges(OUT, this.labels)) {
                        final FaunusVertex vertex = new FaunusVertex(context.getConfiguration(), ((StandardFaunusEdge) edge).getVertexId(IN));
                        vertex.getPaths(value, false);
                        this.longWritable.set(vertex.getLongId());
                        context.write(this.longWritable, this.holder.set('p', vertex));
                        edgesTraversed++;
                    }
                }

                if (this.direction.equals(IN) || this.direction.equals(BOTH)) {
                    for (final Edge edge : value.getEdges(IN, this.labels)) {
                        final FaunusVertex vertex = new FaunusVertex(context.getConfiguration(), ((StandardFaunusEdge) edge).getVertexId(OUT));
                        vertex.getPaths(value, false);
                        this.longWritable.set(vertex.getLongId());
                        context.write(this.longWritable, this.holder.set('p', vertex));
                        edgesTraversed++;
                    }
                }
                value.clearPaths();
                DEFAULT_COMPAT.incrementContextCounter(context, Counters.EDGES_TRAVERSED, edgesTraversed);
            }

            this.longWritable.set(value.getLongId());
            context.write(this.longWritable, this.holder.set('v', value));
        }
    }

    public static class Reduce extends Reducer<LongWritable, Holder, NullWritable, FaunusVertex> {

        @Override
        public void reduce(final LongWritable key, final Iterable<Holder> values, final Reducer<LongWritable, Holder, NullWritable, FaunusVertex>.Context context) throws IOException, InterruptedException {
            final FaunusVertex vertex = new FaunusVertex(context.getConfiguration(), key.get());
            for (final Holder holder : values) {
                final char tag = holder.getTag();
                if (tag == 'v') {
                    vertex.addAll((FaunusVertex) holder.get());
                } else if (tag == 'p') {
                    vertex.getPaths(holder.get(), true);
                } else {
                    vertex.getPaths(holder.get(), false);
                }
            }
            context.write(NullWritable.get(), vertex);
        }
    }
}