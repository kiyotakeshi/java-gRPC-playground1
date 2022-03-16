package com.kiyotakeshi.server;

import com.kiyotakeshi.models.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class BankService extends BankServiceGrpc.BankServiceImplBase {

    @Override
    public void getBalance(BalanceCheckRequest request, StreamObserver<Balance> responseObserver) {

        int accountNumber = request.getAccountNumber();

        Balance balance = Balance.newBuilder()
                .setAmount(AccountDatabase.getBalance(accountNumber))
                .build();

        responseObserver.onNext(balance);
        responseObserver.onCompleted();
    }

    @Override
    public void withdraw(WithdrawRequest request, StreamObserver<Money> responseObserver) {

        int accountNumber = request.getAccountNumber();
        int amount = request.getAmount(); // 10, 20, 30...

        int balance = AccountDatabase.getBalance(accountNumber);

        if(balance < amount) {
            // @see https://developers.google.com/maps-booking/reference/grpc-api-v2/status_codes
            Status status = Status.FAILED_PRECONDITION.withDescription("no enough money. you only have " + balance);
            responseObserver.onError(status.asRuntimeException());
            return;
        }

        // リクエストを分割して処理する
        for (int i = 0; i < (amount / 10); i++) {
            var money = Money.newBuilder().setValue(10).build();
            responseObserver.onNext(money);
            AccountDatabase.deductBalance(accountNumber, 10);
//            try {
//                // 1秒停止を入れることで server-side streaming が動作しているのをわかりやすくする
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }

        responseObserver.onCompleted();
    }
}
