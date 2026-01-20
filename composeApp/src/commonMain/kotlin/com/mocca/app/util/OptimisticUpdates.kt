package com.mocca.app.util

import kotlinx.coroutines.flow.MutableStateFlow

// ═══════════════════════════════════════════════════════════════════════════════
// OPTIMISTIC UPDATES (Priority 4.3)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Utility for optimistic UI updates.
 * 
 * Optimistic updates improve perceived performance by immediately updating the UI
 * before the server confirms the operation. If the operation fails, the UI is
 * reverted to the previous state.
 * 
 * Usage:
 * ```
 * withOptimisticUpdate(
 *     state = _state,
 *     optimisticUpdate = { it.copy(items = it.items + newItem) },
 *     onError = { error -> _state.value = _state.value.copy(error = error.message) }
 * ) {
 *     apiClient.addItem(newItem).getOrThrow()
 * }
 * ```
 */
suspend fun <S, T> withOptimisticUpdate(
    state: MutableStateFlow<S>,
    optimisticUpdate: (S) -> S,
    onError: (Throwable) -> Unit = {},
    block: suspend () -> T
): Result<T> {
    // Capture the original state for potential rollback
    val originalState = state.value
    
    // Apply optimistic update
    state.value = optimisticUpdate(originalState)
    
    return try {
        val result = block()
        Result.success(result)
    } catch (e: Throwable) {
        // Rollback on failure
        state.value = originalState
        onError(e)
        Result.failure(e)
    }
}

/**
 * Extension for simpler optimistic updates on list operations.
 * 
 * Usage:
 * ```
 * state.optimisticAdd(
 *     getList = { it.items },
 *     setList = { state, items -> state.copy(items = items) },
 *     item = newItem
 * ) {
 *     apiClient.addItem(newItem).getOrThrow()
 * }
 * ```
 */
suspend fun <S, T, I> MutableStateFlow<S>.optimisticAdd(
    getList: (S) -> List<I>,
    setList: (S, List<I>) -> S,
    item: I,
    onError: (Throwable) -> Unit = {},
    block: suspend () -> T
): Result<T> {
    return withOptimisticUpdate(
        state = this,
        optimisticUpdate = { currentState ->
            setList(currentState, getList(currentState) + item)
        },
        onError = onError,
        block = block
    )
}

/**
 * Extension for optimistic removal from lists.
 */
suspend fun <S, T, I> MutableStateFlow<S>.optimisticRemove(
    getList: (S) -> List<I>,
    setList: (S, List<I>) -> S,
    predicate: (I) -> Boolean,
    onError: (Throwable) -> Unit = {},
    block: suspend () -> T
): Result<T> {
    return withOptimisticUpdate(
        state = this,
        optimisticUpdate = { currentState ->
            setList(currentState, getList(currentState).filterNot(predicate))
        },
        onError = onError,
        block = block
    )
}

/**
 * Extension for optimistic update of a single item in a list.
 */
suspend fun <S, T, I> MutableStateFlow<S>.optimisticUpdate(
    getList: (S) -> List<I>,
    setList: (S, List<I>) -> S,
    predicate: (I) -> Boolean,
    transform: (I) -> I,
    onError: (Throwable) -> Unit = {},
    block: suspend () -> T
): Result<T> {
    return withOptimisticUpdate(
        state = this,
        optimisticUpdate = { currentState ->
            setList(currentState, getList(currentState).map { 
                if (predicate(it)) transform(it) else it 
            })
        },
        onError = onError,
        block = block
    )
}
