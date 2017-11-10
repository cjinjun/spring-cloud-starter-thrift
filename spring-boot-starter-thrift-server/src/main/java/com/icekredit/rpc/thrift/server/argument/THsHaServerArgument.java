package com.icekredit.rpc.thrift.server.argument;

import com.icekredit.rpc.thrift.server.exception.ThriftServerException;
import com.icekredit.rpc.thrift.server.processor.TRegisterProcessor;
import com.icekredit.rpc.thrift.server.processor.TRegisterProcessorFactory;
import com.icekredit.rpc.thrift.server.properties.THsHaServerProperties;
import com.icekredit.rpc.thrift.server.properties.ThriftServerProperties;
import com.icekredit.rpc.thrift.server.wrapper.ThriftServiceWrapper;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class THsHaServerArgument extends THsHaServer.Args {

    private Map<String, ThriftServiceWrapper> processorMap = new HashMap<>();

    public THsHaServerArgument(List<ThriftServiceWrapper> serviceWrappers, ThriftServerProperties properties)
            throws TTransportException {
        super(new TNonblockingServerSocket(properties.getPort()));

        transportFactory(new TFramedTransport.Factory());
        protocolFactory(new TBinaryProtocol.Factory());

        THsHaServerProperties hsHaProperties = properties.getHsHa();
        minWorkerThreads(hsHaProperties.getMinWorkerThreads());
        maxWorkerThreads(hsHaProperties.getMaxWorkerThreads());

        executorService(createInvokerPool(properties));

        try {
            TRegisterProcessor registerProcessor = TRegisterProcessorFactory.registerProcessor(serviceWrappers);

            processorMap.clear();
            processorMap.putAll(registerProcessor.getProcessorMap());

            processor(registerProcessor);
        } catch (Exception e) {
            throw new ThriftServerException("Can not create multiplexed processor for " + serviceWrappers, e);
        }

    }

    private ExecutorService createInvokerPool(ThriftServerProperties properties) {
        THsHaServerProperties hsHaProperties = properties.getHsHa();

        return new ThreadPoolExecutor(
                hsHaProperties.getMinWorkerThreads(),
                hsHaProperties.getMaxWorkerThreads(),
                hsHaProperties.getKeepAlivedTime(), TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(properties.getWorkerQueueCapacity()));
    }

    public Map<String, ThriftServiceWrapper> getProcessorMap() {
        return processorMap;
    }
}