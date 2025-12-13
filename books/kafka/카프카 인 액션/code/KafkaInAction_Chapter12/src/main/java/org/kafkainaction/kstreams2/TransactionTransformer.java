package org.kafkainaction.kstreams2;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.kafkainaction.Funds;
import org.kafkainaction.Transaction;
import org.kafkainaction.TransactionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;

import static org.kafkainaction.ErrorType.INSUFFICIENT_FUNDS;
import static org.kafkainaction.TransactionType.DEPOSIT;

public class TransactionTransformer implements Processor<String, Transaction, String, TransactionResult> {

  private static final Logger log = LoggerFactory.getLogger(TransactionTransformer.class);

  private final String stateStoreName;
  private KeyValueStore<String, Funds> store;
  private ProcessorContext<String, TransactionResult> context;

  public TransactionTransformer() {
    // default name for funds store
    this.stateStoreName = "fundsStore";
  }

  public TransactionTransformer(final String stateStoreName) {
    this.stateStoreName = stateStoreName;
  }

  @Override
  public void close() {
  }

  private Funds createEmptyFunds(String account) {
    Funds funds = new Funds(account, BigDecimal.ZERO);
    store.put(account, funds);
    return funds;
  }

  private Funds depositFunds(Transaction transaction) {
    return updateFunds(transaction.getAccount(), transaction.getAmount());
  }

  private Funds getFunds(String account) {
    return Optional.ofNullable(store.get(account)).orElseGet(() -> createEmptyFunds(account));
  }

  private boolean hasEnoughFunds(Transaction transaction) {
    return getFunds(transaction.getAccount()).getBalance().compareTo(transaction.getAmount()) != -1;
  }

  @Override
  public void init(ProcessorContext<String, TransactionResult> context) {
    this.context = context;
    this.store = context.getStateStore(stateStoreName);
  }

  @Override
  public void process(Record<String, Transaction> record) {
    String key = record.key();
    Transaction transaction = record.value();
    TransactionResult result;

    if (transaction.getType().equals(DEPOSIT)) {
      result = new TransactionResult(transaction,
                                   depositFunds(transaction),
                                   true,
                                   null);
    } else if (hasEnoughFunds(transaction)) {
      result = new TransactionResult(transaction, withdrawFunds(transaction), true, null);
    } else {
      log.info("Not enough funds for account {}.", transaction.getAccount());
      result = new TransactionResult(transaction,
                                 getFunds(transaction.getAccount()),
                                 false,
                                 INSUFFICIENT_FUNDS);
    }

    context.forward(new Record<>(key, result, record.timestamp()));
  }

  private Funds updateFunds(String account, BigDecimal amount) {
    Funds funds = new Funds(account, getFunds(account).getBalance().add(amount));
    log.info("Updating funds for account {} with {}. Current balance is {}.", account, amount, funds.getBalance());
    store.put(account, funds);
    return funds;
  }

  private Funds withdrawFunds(Transaction transaction) {
    return updateFunds(transaction.getAccount(), transaction.getAmount().negate());
  }
}
