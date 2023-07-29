package redis.clients.jedis;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author wangguangwu
 */
public class CommandArguments implements Iterable<Rawable> {

    private final ArrayList<Rawable> args;

    public CommandArguments(ProtocolCommand command) {
        args = new ArrayList<>();
        args.add(command);
    }

    @Override
    public Iterator<Rawable> iterator() {
        return args.iterator();
    }

    public int size() {
        return args.size();
    }
}
