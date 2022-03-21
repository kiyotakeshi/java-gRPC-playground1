package com.kiyotakeshi.client;

import com.google.common.util.concurrent.Uninterruptibles;
import com.kiyotakeshi.models.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BankClientTest {

    private BankServiceGrpc.BankServiceBlockingStub blockingStub;
    private BankServiceGrpc.BankServiceStub bankServiceStub;

    @BeforeAll
    public void setup() {
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565)
                .usePlaintext()
                .build();

        this.blockingStub = BankServiceGrpc.newBlockingStub(managedChannel);
        this.bankServiceStub = BankServiceGrpc.newStub(managedChannel);
    }

    @Test
    public void balanceTest() {
        BalanceCheckRequest balanceCheckRequest = BalanceCheckRequest.newBuilder()
                .setAccountNumber(5)
                .build();

        Balance balance = this.blockingStub.getBalance(balanceCheckRequest);
        System.out.println("Received : " + balance.getAmount());
    }

    @Test
    public void withdrawTest() {
        // only success at first time
        // second time only remains "30"
        var withdrawRequest = WithdrawRequest.newBuilder().setAccountNumber(8).setAmount(50).build();
        this.blockingStub.withdraw(withdrawRequest)
                .forEachRemaining(money -> System.out.println("received: " + money.getValue()));
    }

    @Test
    void withdrawAsyncTest() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var withdrawRequest = WithdrawRequest.newBuilder().setAccountNumber(6).setAmount(20).build();
        this.bankServiceStub.withdraw(withdrawRequest, new MoneyStreamingResponse(latch));
        latch.await();
        // Thread.sleep(3000);
    }

    @Test
    void cashSteamingRequest() throws InterruptedException {
        var latch = new CountDownLatch(1);
        StreamObserver<DepositRequest> streamObserver = this.bankServiceStub.cashDeposit(new BalanceStreamObserver(latch));
        for (int i = 0; i < 10; i++) {
            var depositRequest = DepositRequest.newBuilder().setAccountNumber(5).setAmount(10).build();
            streamObserver.onNext(depositRequest);
        }
        streamObserver.onCompleted();
        latch.await();
    }
}
