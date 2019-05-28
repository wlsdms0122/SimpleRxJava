public class SimpleRxJava {
    public static void main(String[] args) {
        System.out.println("----------- First Example : Observable.create -----------");
        Observable.create(new Emittable<Integer>() {
            @Override
            public void emit(ObservableType<Integer> emitter) {
                emitter.next(100);
                emitter.next(50);
            }
        }).subscribe(new Subscribable<Integer>() {
            @Override
            public void subscribe(Integer element) {
                System.out.println(element);
            }
        });
        
        System.out.println("----------- Second Example : Error handling -----------");
        Observable.create(new Emittable<Integer>() {
            @Override
            public void emit(ObservableType<Integer> emitter) {
                emitter.next(100);
                emitter.next(50);
            }
        }).error(new ErrorHandler() {
            @Override
            public void onError() {
                System.out.println("onError()");
            }
        }).subscribe(new Subscribable<Integer>() {
            @Override
            public void subscribe(Integer element) {
                System.out.println(element);
            }
        });
        
        System.out.println("----------- Third Example : Map -----------");
        Observable.create(new Emittable<Integer>() {
            @Override
            public void emit(ObservableType<Integer> emitter) {
                emitter.next(100);
                emitter.next(50);
                emitter.error();
                emitter.next(10);
            }
        }).map(new MapTranslatable<Integer, String>() {
            @Override
            public String translate(Integer element) {
                return "map : " + element + " -> " + (element + 10);
            }
        }).subscribe(new Subscribable<String>() {
            @Override
            public void subscribe(String element) {
                System.out.println(element);
            }
        });
        
        System.out.println("----------- Forth Example : FlatMap -----------");
        createIntegerObservable()
        .flatMap(new FlatMapTranslatable<Integer, String>() {
            @Override
            public Observable<String> translate(Integer element) {
                return multiplTwice(element);
            }
        })
        .subscribe(new Subscribable<String>() {
            @Override
            public void subscribe(String element) {
                System.out.println(element);
            }
        });
    }
    
    public static Observable<Integer> createIntegerObservable() {
        return Observable.create(new Emittable<Integer>() {
            @Override
            public void emit(ObservableType<Integer> emitter) {
                emitter.next(10);
                emitter.next(20);
            }
        });
    }
    
    public static Observable<String> multiplTwice(int num) {
        return Observable.create(new Emittable<String>() {
            @Override
            public void emit(ObservableType<String> emitter) {
                emitter.next("flatMap : " + num + " -> " + (num * 2));
            }
        });
    }
}

// Functional interface
interface ObservableType<T> {
    void next(T element);
    void complete();
    void error();
}

interface Emittable<T> {
    void emit(ObservableType<T> emitter);
}

interface MapTranslatable<T, V> {
    V translate(T element);
}

interface FlatMapTranslatable<T, V> {
    Observable<V> translate(T element);
}

interface Subscribable<T> {
    void subscribe(T element);
}

interface ErrorHandler {
    void onError();
}

// Observable class
class Observable<T> implements ObservableType<T> {
    private enum Event {
        next, complete, error
    }
    
    // properties
    private Observable observable;
    
    private Emittable<T> emittable;
    private Subscribable<T> subscribable;
    private ErrorHandler errorHandler;
    
    private T element;
    
    // creater
    public static <T> Observable<T> create(Emittable<T> emittable) {
        return new Observable<>(emittable);
    }
    
    public static Observable<Object[]> zip(final Observable... observables) {
        return Observable.create(new Emittable<Object[]>() {
            private Object[] items = new Object[observables.length];
            private int count = observables.length;
            
            @Override
            public void emit(final ObservableType<Object[]> emitter) {
                for (int i = 0; i < observables.length; i++) {
                    final int index = i;
                    observables[i].subscribe(new Subscribable() {
                        @Override
                        public synchronized void subscribe(Object element) {
                            items[index] = element;
                            
                            count--;
                            if (count == 0) {
                                emitter.next(items);
                            }
                        }
                    });
                }
            }
        });
    }
    
    // constructor
    private Observable(Emittable<T> emittable) {
        this.observable = this;
        this.emittable = emittable;
    }
    
    private void on(Event event) {
        on(event, null);
    }
    
    private void on(Event event, T element) {
        this.element = element;
        
        switch (event) {
            case next:
                if (subscribable != null) {
                    subscribable.subscribe(element);
                }
                break;
            case complete:
                dispose();
                break;
            case error:
                if (errorHandler != null) {
                    errorHandler.onError();
                }
                dispose();
                break;
        }
    }
    
    private void dispose() {
        observable = null;
        emittable = null;
        subscribable = null;
        errorHandler = null;
        element = null;
    }
    
    // operator
    public void next(T element) {
        on(Event.next, element);
    }
    
    public void complete() {
        on(Event.complete);
    }
    
    public void error() {
        on(Event.error);
    }
    
    public Observable<T> error(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }
    
    public void subscribe(Subscribable<T> subscribable) {
        this.subscribable = subscribable;
        emittable.emit(this);
    }
    
    public <R> Observable<R> map(final MapTranslatable<T, R> translatable) {
        return Observable.create(new Emittable<R>() {
            @Override
            public void emit(final ObservableType<R> emitter) {
                errorHandler = ((Observable) emitter).errorHandler;
                subscribe(new Subscribable<T>() {
                    @Override
                    public void subscribe(T element) {
                        emitter.next(translatable.translate(element));
                    }
                });
            }
        });
    }
    
    public <R> Observable<R> flatMap(final FlatMapTranslatable<T, R> translatable) {
        return Observable.create(new Emittable<R>() {
            @Override
            public void emit(final ObservableType<R> emitter) {
                errorHandler = ((Observable) emitter).errorHandler;
                subscribe(new Subscribable<T>() {
                    @Override
                    public void subscribe(T element) {
                        translatable.translate(element)
                        .subscribe(new Subscribable<R>() {
                            @Override
                            public void subscribe(R element) {
                                emitter.next(element);
                            }
                        });
                    }
                });
            }
        });
    }
}
