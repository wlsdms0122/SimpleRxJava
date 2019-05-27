public class SimpleRxJava {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        
        Observable.create(new Emittable<Integer>() {
            @Override
            public void emit(Observable<Integer> emitter) {
                emitter.onNext(100);
                emitter.onNext(50);
            }
        }).map(new MapTranslatable<Integer, String>() {
            @Override
            public String translate(Integer element) {
                return "" + (element + 1);
            }
        }).subscribe(new Subscribable<String>() {
            @Override
            public void subscribe(String element) {
                System.out.println(element);
            }
        });
        
        createIntegerObservable()
        .flatMap(new FlatMapTranslatable<Integer, String>() {
            @Override
            public Observable<String> translate(Integer element) {
                return createConvertStringFromInt(element);
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
            public void emit(Observable<Integer> emitter) {
                emitter.onNext(10);
                emitter.onNext(20);
            }
        });
    }
    
    public static Observable<String> createConvertStringFromInt(int num) {
        return Observable.create(new Emittable<String>() {
            @Override
            public void emit(Observable<String> emitter) {
                emitter.onNext("" + num);
            }
        });
    }
}

// Functional interface
interface Emittable<T> {
    void emit(Observable<T> emitter);
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

// Observable class
class Observable<T> {
    // properties
    private Observable observable;
    
    private Emittable<T> emittable;
    private Subscribable<T> subscribable;
    private T element;
    
    // creater
    public static <T> Observable<T> create(Emittable<T> emittable) {
        return new Observable<>(emittable);
    }
    
    public static Observable<Object[]> concat(Observable... observables) {
        return Observable.create(new Emittable<Object[]>() {
            private Object[] items = new Object[observables.length];
            private int count = observables.length;
            
            @Override
            public void emit(Observable<Object[]> emitter) {
                for (int i = 0; i < observables.length; i++) {
                    final int index = i;
                    observables[i].subscribe(new Subscribable() {
                        @Override
                        public synchronized void subscribe(Object element) {
                            items[index] = element;
                            
                            count--;
                            if (count == 0) {
                                emitter.onNext(items);
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
    
    // operator
    public void onNext(T element) {
        this.element = element;
        if (subscribable != null) {
            subscribable.subscribe(element);
        }
    }
    
    public void subscribe(Subscribable<T> subscribable) {
        this.subscribable = subscribable;
        emittable.emit(this);
    }
    
    public <R> Observable<R> map(MapTranslatable<T, R> translatable) {
        return Observable.create(new Emittable<R>() {
            @Override
            public void emit(final Observable<R> emitter) {
                subscribable = new Subscribable<T>() {
                    @Override
                    public void subscribe(T element) {
                        emitter.onNext(translatable.translate(element));
                    }
                };
                emittable.emit(observable);
            }
        });
    }
    
    public <R> Observable<R> flatMap(FlatMapTranslatable<T, R> translatable) {
        return Observable.create(new Emittable<R>() {
            @Override
            public void emit(final Observable<R> emitter) {
                subscribable = new Subscribable<T>() {
                    @Override
                    public void subscribe(T element) {
                        translatable.translate(element)
                        .subscribe(new Subscribable<R>() {
                            @Override
                            public void subscribe(R element) {
                                emitter.onNext(element);
                            }
                        });
                    }
                };
                emittable.emit(observable);
            }
        });
    }
}
