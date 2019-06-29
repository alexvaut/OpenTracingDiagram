(ns jaeger2diag.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]            
            [schema.core :as s]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.core.strint :refer [<<]]
            [ring.middleware.cors :refer [wrap-cors]]
  )
)
;------------------------
; Schemas

(s/defschema SearchRequest
  {:target s/Str}
)

(s/defschema SearchResult
  [s/Str]
)

(s/defschema QueryRequest
  {:panelId s/Int}  
)

(s/defschema QueryResult
  [{:target s/Str}] 
)

(s/defschema AnnotationRequest
  {}  
)

(s/defschema AnnotationResult
  [{:text s/Str}] 
)

(s/defschema TagKeyResult
  [{:type s/Str
    :text s/Str 
   }] 
)

(s/defschema TagValuesRequest
  {:key s/Str}  
)

(s/defschema TagValuesResult 
  [{:text s/Str}] 
)

;------------------------
; Logic

(defn getTag [span keyName] 
  (:value (first (filter (fn [tag] (= keyName (:key tag))) (:tags span))))  
)

(defn mermaidName [x] 
  (clojure.string/replace x #"[a-z\.]*:\/\/[^\/\s]+\/(.*)" "/$1")
)

(defn buildMessage [span] 
  {:sender (getTag span "sender.path")
    :receiver (getTag span "receiver.path")
    :message (getTag span "message.type") 
    :time (:startTime span) 
  }
)

(defn addToServices 
  ([services id] 
    (let [m (mermaidName id)]
      (if (some #{m} (vals services)) (addToServices services id 1) (assoc services id m))))
  ([services id index] 
    (let [m (str (mermaidName id) "["  index "]" )]
      (if (some #{m} (vals services)) (addToServices services id (+ index 1)) (assoc services id m))))
)

(defn updateServices [services id] 
  (if (get services id) services (addToServices services id))
)

(defn buildNodeNames          
   ([services span & r]      
     ;;(println "S" services "P" span "T" r)  
     (let [newS (updateServices (updateServices services (:sender span)) 
                                (:receiver span)) ]      
      ;;(println "newS" newS "span" span)  
      (if (empty? r) newS (apply buildNodeNames newS r))       
      ))
)

(defn convertServices [serviceMap message] 
    (assoc 
        (assoc message               
        :sender (get serviceMap (:sender message)))
        :receiver (get serviceMap (:receiver message)))    
)

(defn sortMessages [messages]
   (sort (fn [a b] (compare (:time a) (:time b))) messages)
)

(defn getSpans [traces]    
   (let [messages (sortMessages (map buildMessage (apply concat (map (fn [trace] (:spans trace)) traces))))]    
     ;;messages
     (let [serviceMap (apply buildNodeNames {} messages)]  
       (println "************************************")
       (map (fn [m] (convertServices serviceMap m)) messages)
     )
   )
)

(defn mermaidMessageType [x] 
  (clojure.string/replace x #".*(\.|\+)([^.\+]*)" "$2")
)

(defn mermaidjsfy [message] 
  (str "\t" (:sender message) "->>" (:receiver message)  ":"  (mermaidMessageType (:message message))  "\"\n" )
)

(defn serviceGraph [tracesStr] 
  (let 
    [traces (:data (json/read-str tracesStr :key-fn keyword))]  
    (println "Traces count: "(count traces))
    (if (empty? traces) 
      "sequenceDiagram\nEmpty->>Empty:_\n" 
      (apply str "sequenceDiagram\n" (map mermaidjsfy (getSpans traces)))      
    )
    ;;(apply str "sequenceDiagram\n" (getSpans traces))
  )  
)

(defn getServices []
  (:data (json/read-str (:body (client/get "http://localhost:16686/api/services" ) {:accept :json}) :key-fn keyword))
)

;------------------------
; APIs

(def app
  (->
    (api
      {:swagger
      {:ui "/"
        :spec "/swagger.json"
        :data {:info {:title "Open Tracing Diagram API"
                      :description "Open access to diagrams built from Open Traces (jaeger)."}
              :tags [{:name "grafana", :description "API used as a source by Grafana."}
                     {:name "diagram", :description "API used to render diagrams."}              
              ]}}}
      
      (context "/grafana" []      
      :tags ["grafana"]

        (GET "/" []
          :return String     
          :summary "Grafana test connection" 
          (println "/home")
          (ok   "ok")
        )

        (POST "/search" []
          :return SearchResult
          :body [searchRequest SearchRequest]
          :summary "search time series" 
           (println "/search" searchRequest)
          (ok (if (= (:target searchRequest) "services") (getServices) ["A" "B"]  )
          ) ;;empty array
        )

        (POST "/query" []
          :return QueryResult
          :body [queryRequest QueryRequest]
          :summary "query time series" 
          (println "/query" queryRequest)
          (ok []) ;;empty array
        )
 
        (POST "/annotations" []
          :return AnnotationResult
          :body [annotationRequest AnnotationRequest]
          :summary "Annotation" 
          (println "/annotations" annotationRequest)
          (ok []) ;;empty array
        )

        (POST "/tag-keys" []
          :return TagKeyResult          
          :summary "Tag keys" 
          (println "/tag-keys")
          (ok [{:type "string" :text "Services"}]) ;;empty array
        )

        (POST "/tag-values" []
          :return TagValuesResult  
          :body [tagValuesRequest TagValuesRequest]        
          :summary "Annotation" 
          (println "/tag-values" tagValuesRequest)
          (ok [{:text "service1"} {:text "service2"}]) ;;empty array
        )
      )
        
      (context "/" []      
      :tags ["diagram"]

        (GET "/sequence" []
          :return String
          :query-params [limit :- Long, service :- String, startMilliS :- Long, endMilliS :- Long]
          :summary "provide a sequence diargram (Mermaid format)."        
          (println "Service" service)
          (serviceGraph
            (:body                 
              (client/get 
                (let [startNanoS (* startMilliS 1000) endNanoS (* endMilliS 1000)]                  
                  (let [url (<< "http://localhost:16686/api/traces?end=~{endNanoS}&limit=~{limit}&lookback=1h&maxDuration&minDuration&service=~{service}&start=~{startNanoS}" )]
                    (println url)
                    url
                  )
                )              
                {:accept :json}
              )
            )
          ) 
        )
      )    
    )
    (wrap-cors :access-control-allow-origin #".*"
              :access-control-allow-headers ["Origin" "X-Requested-With" "Content-Type" "Accept"]
              :access-control-allow-methods [:get :put :post :delete :options])
))