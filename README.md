# 设计思路
通过分析购票系统，本并发购票数据结构将之拆分成四个类。  
其中 TicketingDS 类作为与外接的接口，其余三个分别为 RouteNode-车次, CoachNode-车厢, SeatNode-座位。

## TicketingDS 类
TicketingDS 类的私有属性是一个 routeList 和 routeNum，包含三个方法分别是
public Ticket buyTicket(String passenger, int route, int departure, int arrival)
public int inquiry(int route, int departure, int arrival)
public boolean refundTicket(Ticket ticket)
实现的是 TicketingSystem 接口中的方法。分别是购票，查询当前余票和退票。

## RouteNode 类
RouteNode 是一个 coachList， coachNum 和 routeId，具体实现上述三个方法。

## CoachNode 类
CoachNode 维护一个 seatList， seatNum 和 coachId，具体实现上述三个方法。

## SeatNode 类
SeatNode 维护一个 seatId 和一个 AtomicLong 型 64 位的 availableSeat， 
availableSeat 的每一位表示座位对应的每一站， 0 表示未售出， 1 表示售出。
购票查询退票时均采用从 route-\>coach-\>seat 的方式调用方法，在 seatNode 操作时，
用原语 compareAndSet 构造非阻塞式的自旋锁来保证并发操作的原子性。
