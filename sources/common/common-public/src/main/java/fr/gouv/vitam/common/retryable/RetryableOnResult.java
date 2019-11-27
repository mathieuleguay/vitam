/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.retryable;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RetryableOnResult<T, E extends Exception> implements Retryable<T, E> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RetryableOnResult.class);
    private static final Consumer NOOP = r -> {};

    private final AtomicInteger counter = new AtomicInteger();
    private final Random randomSleep = new Random();

    private final Predicate<T> retryOn;
    private final Consumer<T> onResult;

    private RetryableParameters param;

    public RetryableOnResult(RetryableParameters param, Predicate<T> retryOn, Consumer<T> onResult) {
        this.param = Objects.requireNonNull(param);
        this.retryOn = Objects.requireNonNull(retryOn);
        this.onResult = Objects.requireNonNull(onResult);
    }

    public RetryableOnResult(RetryableParameters param, Predicate<T> retryOn) {
        this(param, retryOn, NOOP);
    }

    @Override
    public T exec(DelegateRetry<T, E> delegate) throws E {
        while (counter.getAndIncrement() < param.getNbRetry()) {
            LOGGER.warn("Retryable='{}' - Attempt '{}' of '{}'.", delegate.toString(), counter.get(), param.getNbRetry());
            T result = delegate.call();
            if (counter.get() >= param.getNbRetry() || retryOn.negate().test(result)) {
                LOGGER.warn("Retryable='{}' - Stop retry at attempt '{}' and return result of type '{}'.", counter.get(), result.getClass().getSimpleName());
                return result;
            }
            onResult.accept(result);
            sleep(counter.get(), delegate.toString(), LOGGER, param, randomSleep);
        }

        throw new IllegalStateException("Unreachable statement.");
    }

    @Override
    public void execute(DelegateRetryVoid<E> delegate) throws E {
        throw new IllegalStateException("Cannot use this function");
    }
}